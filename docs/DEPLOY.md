# HospitOps — VPS Production Deployment

This is the runbook for the single-VPS production baseline (Phase 0.2 of the
evolution plan). It deploys the React SPA + Spring Boot backend + PostgreSQL +
Redis behind Caddy (automatic TLS), with daily database backups.

For local development use `docker-compose.yml` instead (see the repo README).

---

## Topology

```
internet ──443/80──> caddy (TLS) ──> frontend (nginx, React SPA)
                                          │  /api/*  ──> app (Spring Boot)
                                          ▼
                              app ──> postgres   (internal only)
                              app ──> redis      (internal only)
                              postgres ──> postgres-backup (daily pg_dump)
```

Only Caddy binds host ports (80/443). Everything else lives on the internal
Compose network and is unreachable from the internet.

Files:
- `docker-compose.prod.yml` — the production stack
- `Caddyfile` — TLS edge + reverse proxy
- `.env.prod.example` — secret template (copy to `.env.prod`)

---

## 1. Provision the VPS

Recommended tier (15 hotels / ~280 rooms, pre-channel-sync): **2 vCPU / 4 GB /
80 GB NVMe**. Once channel sync is live, move to **4 vCPU / 8 GB / 160 GB**.
Host in **Jakarta or Singapore (ap-southeast-1)** to keep latency low for
Indonesian staff and OTA sync.

On the VPS:
- Install Docker Engine + the Compose plugin.
- Open firewall ports **80** and **443** (TCP, and 443 UDP for HTTP/3).
- Create a non-root user in the `docker` group; run the stack as that user.
- Enable swap (e.g. 2 GB) and unattended security updates.

## 2. DNS

Create an **A** record (and **AAAA** if you have IPv6) for your domain pointing
at the VPS public IP. This must resolve **before** first boot — Caddy validates
the domain with Let's Encrypt over HTTP, which fails without correct DNS.

## 3. Configure secrets

```bash
git clone <repo> hospitops && cd hospitops
cp .env.prod.example .env.prod
```

Edit `.env.prod` and set real values. Generate strong secrets:

```bash
openssl rand -base64 48   # JWT_SECRET   (>= 32 chars required)
openssl rand -base64 24   # POSTGRES_PASSWORD
openssl rand -base64 24   # REDIS_PASSWORD
```

Set `DOMAIN`, `ACME_EMAIL`, `GHCR_OWNER`, and pin `APP_IMAGE_TAG` to a specific
commit SHA for reproducible deploys (avoid `latest` in production).

`.env.prod` is gitignored — never commit it.

## 4. Authenticate to the image registry (if private)

The backend image is pulled from `ghcr.io/<owner>/hotel-backend`. If the package
is private:

```bash
echo "$GHCR_PAT" | docker login ghcr.io -u <github-user> --password-stdin
```

(`GHCR_PAT` = a GitHub PAT with `read:packages`.) Public packages need no login.

## 5. Bring it up

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d
```

First boot: Caddy obtains a certificate (a few seconds), the app runs Flyway
migrations and seeds the default group/hotel. Watch progress:

```bash
docker compose -f docker-compose.prod.yml logs -f app caddy
```

## 6. Verify

```bash
# TLS + SPA
curl -sI https://$DOMAIN/ | head -1                     # HTTP/2 200
# API through the edge
curl -s https://$DOMAIN/api/v1/group/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"groupadmin@hospitops.local","password":"admin123"}' | head -c 200
```

> **First task after a fresh deploy:** log in and change the seeded credentials
> (`groupadmin@hospitops.local` / `admin123` and staff `admin` / `admin123`).
> They exist only to bootstrap and must not survive into real production use.

---

## Updating the backend

```bash
APP_IMAGE_TAG=<new-sha> docker compose --env-file .env.prod \
  -f docker-compose.prod.yml up -d app
```

Native image boot is near-instant; Flyway applies any new migrations on start.
Roll back by re-running with the previous SHA.

## Updating the frontend

The frontend is built from source by Compose:

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build frontend
```

---

## Backups & restore

`postgres-backup` writes compressed dumps to the `postgres-backups` volume on
the `BACKUP_SCHEDULE` (default `@daily`), with daily/weekly/monthly retention.

**List backups:**
```bash
docker compose -f docker-compose.prod.yml exec postgres-backup ls -lh /backups/daily
```

**Copy a backup off the box (do this regularly — a backup on the same VPS is
not a backup):**
```bash
docker compose -f docker-compose.prod.yml cp \
  postgres-backup:/backups/daily/ ./db-backups/
```
Better: sync `./db-backups/` to off-site object storage (S3/B2) via cron.

**Restore drill** (rehearse on staging before you need it for real):
```bash
# 1. Stop the app so nothing writes during restore
docker compose -f docker-compose.prod.yml stop app
# 2. Pipe a chosen dump back into postgres
gunzip -c ./db-backups/daily/<file>.sql.gz | \
  docker compose -f docker-compose.prod.yml exec -T postgres \
    psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"
# 3. Restart the app
docker compose -f docker-compose.prod.yml start app
```

---

## Secret rotation

- **JWT_SECRET** — rotating it invalidates all active sessions (everyone is
  logged out). Update `.env.prod`, then `up -d app`.
- **POSTGRES_PASSWORD** — change inside Postgres (`ALTER USER ... PASSWORD`) and
  in `.env.prod` together, then `up -d`.
- **REDIS_PASSWORD** — update `.env.prod` and `up -d redis app` (both read it).

Rotate after any suspected exposure and on a periodic schedule.

---

## Monitoring

The backend exposes `/actuator/health/readiness` and `/actuator/health/liveness`
(internal only — reached via the app container or through Caddy if you choose to
expose them). The app container has **no Docker healthcheck** because the GraalVM
runtime image is minimal (no shell/curl); use an external uptime monitor against
`https://$DOMAIN/` and, if you enable it, scrape `/actuator/prometheus`
(`PROMETHEUS_ENABLED=true`).

---

## Notes

- **Staging:** keep a small (2 vCPU / 4 GB) staging VPS running the same stack to
  rehearse upgrades, restores, and (later) the channel-sync cutover.
- **Database isolation:** once OTA bookings are mission-critical, move PostgreSQL
  to its own VPS or managed Postgres with PITR — a corrupted DB holding live OTA
  reservations is the worst-case failure.
- **Kubernetes** manifests under `hotel-backend/k8s/` are parked for future
  scale-out and are not used by this VPS baseline.
