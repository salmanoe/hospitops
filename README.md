# HospitOps

[![CI](https://github.com/salmanoe/hospitops/actions/workflows/ci.yml/badge.svg)](https://github.com/salmanoe/hospitops/actions/workflows/ci.yml)
[![Build](https://github.com/salmanoe/hospitops/actions/workflows/build.yml/badge.svg)](https://github.com/salmanoe/hospitops/actions/workflows/build.yml)
[![Deploy](https://github.com/salmanoe/hospitops/actions/workflows/deploy.yml/badge.svg)](https://github.com/salmanoe/hospitops/actions/workflows/deploy.yml)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=salmanoe_hospitops&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=salmanoe_hospitops)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=salmanoe_hospitops&metric=coverage)](https://sonarcloud.io/summary/new_code?id=salmanoe_hospitops)
[![Java](https://img.shields.io/badge/Java-25_LTS-orange?logo=openjdk)](https://openjdk.org/projects/jdk/25/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue)](LICENSE)

Hotel management platform for hotel groups and individual properties. Manages group-level administration and per-hotel operations — staff identity, room management, guest profiles, reservations, housekeeping, and billing — built as a clean-architecture Spring Boot monolith designed to evolve into Spring Modulith and then microservices without structural rewrites.

---

## Repository Structure

```
hospitops/
├── hotel-backend/          ← Java 25 · Spring Boot 4.0.6 · GraalVM Native Image
│   ├── shared/             ← Typed IDs, Money, TaxPolicy, Guard, Domain Events, API wrappers
│   ├── group/              ← Group profile, GROUP_ADMIN accounts, self-service signup
│   ├── hotel/              ← Hotel profile, lifecycle, setup wizard, hotel_summary
│   ├── identity/           ← Staff auth, JWT access tokens, refresh tokens
│   ├── room/               ← Rooms, room types, seasonal rate overrides
│   ├── guest/              ← Guest profiles, search
│   ├── reservation/        ← Booking, check-in, check-out, cancellation
│   ├── housekeeping/       ← Room status board, task management
│   ├── billing/            ← Invoices, payments, PDF export (iText 9)
│   ├── bootstrap/          ← App entry point, Security, Flyway migrations, Redis config
│   ├── coverage-aggregate/ ← JaCoCo multi-module aggregate report
│   └── k8s/                ← Kubernetes manifests (production)
├── hotel-frontend/         ← Static HTML/CSS/JS · Nginx config
├── docker-compose.yml      ← Full local dev stack (run from here)
└── .env.example            ← Environment variable template
```

**Stack:** Java 25 LTS · Spring Boot 4.0.6 · GraalVM Native Image (Liberica NIK) · PostgreSQL 17 · Redis 7 · Nginx · Docker Compose (dev) · Kubernetes (prod) · GHCR (container registry)

---

## Environments at a Glance

| Environment | Tooling        | Command                         | Purpose                            |
|-------------|----------------|---------------------------------|------------------------------------|
| Development | Docker Compose | `docker compose up`             | Full local stack, hot-reload ready |
| CI          | GitHub Actions | automatic on push               | Tests, coverage, quality gate, nginx validation |
| Staging     | Kubernetes     | auto-deploy after merge to main | Integration testing in-cluster     |
| Production  | Kubernetes     | manual approval gate            | Live traffic                       |

---

## Development — Docker Compose

Docker Compose is the **only tool you need** to run the full stack locally. Run all commands from the **repo root** (`hospitops/`). One command starts PostgreSQL, Redis, the Spring Boot backend, and the Nginx frontend together.

### Prerequisites

```bash
# Docker Desktop 4.x+ or Docker Engine + Compose plugin v2.x
docker compose version   # must be v2.x

# Java (only needed to run Gradle commands directly outside Docker).
# Gradle itself is provided by the wrapper (./gradlew) — no separate install.
sdk install java 25.r25-nik   # GraalVM 25 via SDKMAN (Liberica NIK)
```

### Start the full stack

```bash
# From repo root: hospitops/
docker compose up          # start all services, stream logs
docker compose up -d       # detached (background)
docker compose up --build  # rebuild the backend image first
```

The app is at **http://localhost** — Nginx serves the frontend and proxies `/api/*` to Spring Boot. First run takes ~90 seconds while Flyway applies all migrations.

### Compose services

| Service      | Description                                    | Exposed            |
|--------------|------------------------------------------------|--------------------|
| `postgres`   | PostgreSQL 17 (data in named volume)           | Port 5432 (IDE)    |
| `redis`      | Redis 7 (token blacklist + refresh token store)| Internal only      |
| `app`        | Spring Boot backend (dev profile)              | Internal (via Nginx)|
| `frontend`   | Nginx serving hotel-frontend/, proxying /api/* | http://localhost   |
| `sonarqube`  | SonarQube CE (optional — see below)            | Port 9000          |

### Stop and clean up

```bash
docker compose down        # stop containers, keep DB/Redis data
docker compose down -v     # stop containers AND delete all data volumes
```

### Useful commands during development

```bash
docker compose logs -f app       # tail backend logs
docker compose logs -f frontend  # tail nginx logs
docker compose restart app       # reload backend after a code change

# Open a psql shell in the running postgres container
docker compose exec postgres psql -U hotel_user -d hotel_db

# Run Gradle commands directly (requires postgres running via Compose)
cd hotel-backend
./gradlew test
./gradlew :bootstrap:bootRun
```

### SonarQube (optional)

SonarQube is defined in Compose but excluded from the default profile. Start it only when you need a local quality analysis:

```bash
# From repo root
docker compose --profile sonar up -d sonarqube

# First login: admin / admin (SonarQube will prompt to change it)
# Then generate a token under My Account → Security, and run from hotel-backend/:
./gradlew build sonar \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token=YOUR_LOCAL_TOKEN
```

---

## Environment Variables (`.env`)

Compose reads `.env` from the repo root. The file is gitignored — copy the template to get started:

```bash
cp .env.example .env
```

| Variable                        | Default                | Description                                      |
|---------------------------------|------------------------|--------------------------------------------------|
| `POSTGRES_DB`                   | `hotel_db`             | Database name                                    |
| `POSTGRES_USER`                 | `hotel_user`           | Database user                                    |
| `POSTGRES_PASSWORD`             | `yourpassword`         | Database password                                |
| `JWT_SECRET`                    | *(see .env.example)*   | Min 32 chars. App refuses to start if too short. |
| `JWT_EXPIRATION_MS`             | `28800000`             | Access token TTL (8 hours)                       |
| `REFRESH_TOKEN_EXPIRATION_SECONDS` | `604800`            | Refresh token TTL (7 days)                       |
| `REDIS_ENABLED`                 | `false`                | `true` → Redis-backed token blacklist            |
| `REDIS_HOST`                    | `localhost`            | Redis host (Compose service name: `redis`)       |
| `REDIS_PORT`                    | `6379`                 | Redis port                                       |
| `REDIS_PASSWORD`                | *(empty)*              | Redis password (empty for local dev)             |
| `FRONTEND_URL`                  | `http://localhost:5500`| Allowed CORS origin                              |
| `LOG_LEVEL`                     | `DEBUG`                | Log level for `id.co.hospitops` packages         |
| `DB_POOL_MAX`                   | `15`                   | HikariCP maximum pool size                       |

> ⚠️ `JWT_SECRET` must be overridden in every deployed environment. Generate with: `openssl rand -base64 48`

---

## Quick Smoke Test

After `docker compose up`, verify the stack is live:

```bash
# Frontend — should open the login page
open http://localhost

# Backend health (proxied through Nginx)
curl http://localhost/actuator/health

# Hotel staff login — username + password only; returns an access token and a
# refresh token. The staff member's hotel is carried inside the issued JWT.
curl -X POST http://localhost/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# GROUP_ADMIN login
curl -X POST http://localhost/api/v1/group/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"groupadmin","password":"groupadmin123"}'
```

---

## Default Credentials

### Hotel staff (scoped to the seed hotel)

| Username      | Password        | Role         |
|---------------|-----------------|--------------|
| `admin`       | `admin123`      | Admin        |
| `manager`     | `manager123`    | Manager      |
| `frontdesk1`  | `frontdesk123`  | Front Desk   |
| `housekeeper` | `hk123456`      | Housekeeping |
| `accountant`  | `acc12345`      | Accountant   |

Login endpoint: `POST /api/v1/auth/login` (the hotel is resolved from the account and embedded in the JWT)

### Group admin

| Username      | Password          | Role         |
|---------------|-------------------|--------------|
| `groupadmin`  | `groupadmin123`   | GROUP_ADMIN  |

Login endpoint: `POST /api/v1/group/auth/login`

> ⚠️ Change all passwords before deploying to any shared environment.

---

## Testing & Quality

```bash
cd hotel-backend

./gradlew test                    # all module tests (domain + application + web slice + integration)
./gradlew build                   # compile, test, and JaCoCo coverage reports
./gradlew build sonar \           # SonarQube analysis (requires running instance)
  -Dsonar.host.url=... \
  -Dsonar.token=...
```

Coverage targets enforced by SonarQube Quality Gate:
`Domain 90%` · `Application 80%` · `Web 70%` · `Overall 75%`

---

## Build

```bash
cd hotel-backend

./gradlew :bootstrap:bootJar                  # fat JAR (build/libs/bootstrap-*.jar)
./gradlew :bootstrap:nativeCompile            # native binary (~5–15 min)
./gradlew :bootstrap:bootBuildImage \         # native Docker image
  --imageName=ghcr.io/<owner>/hotel-backend:<tag>
```

| Mode                  | Startup  | Memory  | Notes                     |
|-----------------------|----------|---------|---------------------------|
| JVM + virtual threads | ~4s      | ~300 MB | Default for local dev      |
| GraalVM native        | ~80ms    | ~70 MB  | ~15% lower peak throughput |

---

## CI/CD Pipeline

```
┌─────────────┐   ┌──────────────────────────┐   ┌──────────────────────┐   ┌───────────────────────┐
│  Code Push  │──▶│  CI  (ci.yml)            │──▶│  Build (build.yml)   │──▶│  Deploy (deploy.yml)  │
│  or PR      │   │                          │   │                      │   │                       │
└─────────────┘   │  ✓ Compile               │   │  ✓ Install modules   │   │  → Staging (auto)     │
                  │  ✓ Unit tests            │   │  ✓ Native image      │   │                       │
                  │  ✓ JaCoCo coverage       │   │  ✓ Push to GHCR      │   │  → Production         │
                  │  ✓ SonarQube gate        │   │  ✓ Smoke test        │   │    (manual approval)  │
                  │  ✓ Nginx config check    │   │                      │   │                       │
                  └──────────────────────────┘   └──────────────────────┘   └───────────────────────┘
```

| Workflow     | Trigger                    | Purpose                                              |
|--------------|----------------------------|------------------------------------------------------|
| `ci.yml`     | Every push + PR            | Tests, coverage, SonarQube gate, nginx validation    |
| `build.yml`  | Merge to `main` (or manual)| Native image build → GHCR (`ghcr.io/<owner>/hotel-backend`) |
| `deploy.yml` | After build                | K8s rolling update; manual gate for production       |

### GitHub Actions Secrets

Go to **Settings → Secrets and variables → Actions** and add:

| Secret                   | Description                                             |
|--------------------------|---------------------------------------------------------|
| `SONAR_TOKEN`            | SonarQube → Account → Security → Token                 |
| `SONAR_HOST_URL`         | SonarQube server URL (analysis is skipped if not set)   |
| `KUBE_CONFIG_STAGING`    | `cat ~/.kube/config \| base64` (staging cluster)        |
| `KUBE_CONFIG_PRODUCTION` | `cat ~/.kube/config \| base64` (production cluster)     |

> `GITHUB_TOKEN` is automatically available — no setup needed for GHCR image pushes.

Go to **Settings → Environments** and create:

- `staging` — no restrictions (auto-deploy on every merge to `main`)
- `production` — add **Required reviewers** for the manual approval gate

---

## Production — Kubernetes

Kubernetes manifests live in `hotel-backend/k8s/`. Apply them in order:

```bash
kubectl apply -f hotel-backend/k8s/namespace.yaml
kubectl apply -f hotel-backend/k8s/postgres.yaml
kubectl apply -f hotel-backend/k8s/app.yaml
kubectl apply -f hotel-backend/k8s/ingress-and-netpol.yaml

# Optional — only if running self-hosted SonarQube in-cluster
kubectl apply -f hotel-backend/k8s/sonarqube.yaml
```

### Before you deploy

1. **Update the image tag** in `k8s/app.yaml`:
   ```yaml
   image: ghcr.io/salmanoe/hotel-backend:<sha>
   ```

2. **Rotate all secrets** — the defaults in the manifests are base64-encoded placeholders. Never deploy them as-is:
   ```bash
   echo -n "your-real-password" | base64
   ```
   In production, prefer [Sealed Secrets](https://github.com/bitnami-labs/sealed-secrets) or [External Secrets Operator](https://external-secrets.io) over plain Kubernetes Secrets.

3. **Update the domain** in `k8s/ingress-and-netpol.yaml`:
   ```yaml
   host: hotel.yourdomain.com
   ```

4. **Install cluster prerequisites** (if not already present):
   ```bash
   # Ingress controller
   kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/cloud/deploy.yaml

   # cert-manager for automatic TLS via Let's Encrypt
   kubectl apply -f https://github.com/cert-manager/cert-manager/releases/latest/download/cert-manager.yaml
   ```

### Verify the deployment

```bash
kubectl rollout status deployment/hotel-backend -n hotel
kubectl get pods -n hotel
kubectl get hpa -n hotel
kubectl logs -f deployment/hotel-backend -n hotel
```

### Production resource profile (native image)

| Component     | CPU request | CPU limit | Memory request | Memory limit |
|---------------|-------------|-----------|----------------|--------------|
| hotel-backend | 100m        | 500m      | 64Mi           | 128Mi        |
| postgres      | 250m        | 1000m     | 256Mi          | 512Mi        |

The HPA scales the app between 2 and 10 replicas on CPU > 70% or memory > 80%. Native image cold start < 100ms makes scale-out near-instant. Pod anti-affinity ensures the two baseline replicas land on separate nodes.

---

## License

MIT
