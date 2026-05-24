# HospitOps

[![CI](https://github.com/salmanoe/hospitops/actions/workflows/ci.yml/badge.svg)](https://github.com/salmanoe/hospitops/actions/workflows/ci.yml)
[![Build](https://github.com/salmanoe/hospitops/actions/workflows/build.yml/badge.svg)](https://github.com/salmanoe/hospitops/actions/workflows/build.yml)
[![Deploy](https://github.com/salmanoe/hospitops/actions/workflows/deploy.yml/badge.svg)](https://github.com/salmanoe/hospitops/actions/workflows/deploy.yml)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=salmanoe_hospitops&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=salmanoe_hospitops)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=salmanoe_hospitops&metric=coverage)](https://sonarcloud.io/summary/new_code?id=salmanoe_hospitops)
[![Java](https://img.shields.io/badge/Java-25_LTS-orange?logo=openjdk)](https://openjdk.org/projects/jdk/25/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue)](LICENSE)

Hotel management system for a medium hotel (20–100 rooms). A clean-architecture Spring Boot backend served behind an Nginx frontend, designed to evolve from monolith → Spring Modulith → microservices without structural rewrites.

---

## Repository Structure

```
hospitops/
├── hotel-backend/          ← Java 25 · Spring Boot 4 · GraalVM Native Image
│   ├── shared/             ← Typed IDs, Money, Domain Events, ApiResponse
│   ├── identity/           ← Staff auth, JWT                      ✅ done
│   ├── room/               ← Rooms, types, seasonal pricing       🔲 batch 2
│   ├── guest/              ← Guest profiles, search               🔲 batch 2
│   ├── reservation/        ← Booking, check-in, check-out         🔲 batch 3
│   ├── housekeeping/       ← Room status board, tasks             🔲 batch 3
│   ├── billing/            ← Invoices, payments, PDF export       🔲 batch 4
│   ├── bootstrap/          ← App entry, Security, Flyway          ✅ done
│   ├── coverage-aggregate/ ← JaCoCo multi-module report           ✅ done
│   └── k8s/                ← Kubernetes manifests (prod)
├── hotel-frontend/         ← Static HTML/CSS/JS · Nginx
├── docker-compose.yml      ← Full local dev stack (run from here)
└── .env.example            ← Environment variable template
```

**Stack:** Java 25 LTS · Spring Boot 4 · GraalVM Native Image · PostgreSQL 17 · Nginx · Docker Compose (dev) · Kubernetes (prod)

---

## Environments at a Glance

| Environment | Tooling        | Command                         | Purpose                            |
|-------------|----------------|---------------------------------|------------------------------------|
| Development | Docker Compose | `docker compose up`             | Full local stack, hot-reload ready |
| CI          | GitHub Actions | automatic on push               | Tests, coverage, quality gate      |
| Staging     | Kubernetes     | auto-deploy after merge to main | Integration testing in-cluster     |
| Production  | Kubernetes     | manual approval gate            | Live traffic                       |

---

## Development — Docker Compose

Docker Compose is the **only tool you need** to run the full stack locally. Run all commands from the **repo root** (`hospitops/`). One command starts PostgreSQL, the Spring Boot backend, and the Nginx frontend together.

### Prerequisites

```bash
# Docker Desktop 4.x+ or Docker Engine + Compose plugin
docker compose version   # must be v2.x

# Java + Maven (only needed to run Maven commands directly)
sdk install java 25.r25-nik   # GraalVM 25 via SDKMAN (Liberica NIK)
sdk install maven
```

### Start the full stack

```bash
# From repo root: hospitops/
docker compose up          # start postgres + backend + frontend, stream logs
docker compose up -d       # detached (background)
docker compose up --build  # rebuild the backend image first
```

The app is at **http://localhost** — Nginx serves the frontend and proxies `/api/*` to Spring Boot.

### Compose services

```
postgres    → PostgreSQL 17 (internal, data persisted in a named volume)
app         → Spring Boot backend (dev profile, connects to postgres)
frontend    → Nginx serving hotel-frontend/, proxying /api/* → app:8080
sonarqube   → SonarQube CE (port 9000, optional — see below)
```

### Stop and clean up

```bash
docker compose down        # stop containers, keep DB data
docker compose down -v     # stop containers AND delete all DB data
```

### Useful commands during development

```bash
docker compose logs -f app       # tail backend logs
docker compose logs -f frontend  # tail nginx logs
docker compose restart app       # reload backend after a code change

# Open a psql shell in the running postgres container
docker compose exec postgres psql -U hotel_user -d hotel_db

# Run Maven commands directly (requires postgres already running via Compose)
cd hotel-backend
mvn test -pl bootstrap -am -Pdev
mvn spring-boot:run -pl bootstrap -am -Pdev
```

### SonarQube (optional)

SonarQube is defined in Compose but excluded from the default profile. Start it only when you need a local quality analysis:

```bash
# From repo root
docker compose --profile sonar up -d sonarqube

# Then from hotel-backend/
mvn sonar:sonar -Psonar \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token=YOUR_LOCAL_TOKEN
```

---

## Environment Variables (`.env`)

Compose reads `.env` from the repo root. The file is gitignored — copy the template to get started:

```bash
cp .env.example .env
```

| Variable            | Default              | Description              |
|---------------------|----------------------|--------------------------|
| `POSTGRES_DB`       | `hotel_db`           | Database name            |
| `POSTGRES_USER`     | `hotel_user`         | Database user            |
| `POSTGRES_PASSWORD` | `yourpassword`       | Database password        |
| `JWT_SECRET`        | *(see .env.example)* | Min 32 chars             |
| `JWT_EXPIRATION_MS` | `28800000`           | Token TTL (8 hours)      |
| `LOG_LEVEL`         | `DEBUG`              | Log level for dev profile|

---

## Quick Smoke Test

After `docker compose up`, verify the stack is live:

```bash
# Frontend
open http://localhost          # login page

# Health check (proxied through nginx)
curl http://localhost/actuator/health

# Login
curl -X POST http://localhost/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

---

## Default Credentials

| Username      | Password       | Role         |
|---------------|----------------|--------------|
| `admin`       | `admin123`     | Admin        |
| `manager`     | `manager123`   | Manager      |
| `frontdesk1`  | `frontdesk123` | Front Desk   |
| `housekeeper` | `hk123456`     | Housekeeping |
| `accountant`  | `acc12345`     | Accountant   |

> ⚠️ Change all passwords before deploying to any shared environment: `PATCH /api/v1/staff/{id}/password`

---

## Testing & Quality

```bash
cd hotel-backend
mvn test                          # unit tests (domain + app + web slice)
mvn clean verify -Pcoverage       # tests + JaCoCo coverage report
mvn sonar:sonar -Psonar \         # SonarQube analysis (requires running instance)
  -Dsonar.host.url=... \
  -Dsonar.token=...
```

Coverage targets enforced by SonarQube Quality Gate:
`Domain 90%` · `Application 80%` · `Web 70%` · `Overall 75%`

---

## Build

```bash
cd hotel-backend
mvn package -pl bootstrap -am -DskipTests           # fat JAR
mvn -Pnative native:compile -pl bootstrap -am       # native binary (~5 min)
mvn -Pnative spring-boot:build-image -pl bootstrap -am  # native Docker image
```

| Mode                  | Startup | Memory | Throughput      |
|-----------------------|---------|--------|-----------------|
| JVM + virtual threads | ~4s     | ~300MB | Baseline        |
| GraalVM native        | ~80ms   | ~70MB  | ~15% lower peak |

---

## CI/CD Pipeline

```
┌─────────────┐   ┌─────────────────────┐   ┌──────────────────────┐   ┌───────────────────────┐
│  Code Push  │──▶│  CI  (ci.yml)       │──▶│  Build (build.yml)   │──▶│  Deploy (deploy.yml)  │
│  or PR      │   │                     │   │                      │   │                       │
└─────────────┘   │  ✓ Compile          │   │  ✓ Spring AOT        │   │  → Staging (auto)     │
                  │  ✓ Unit tests       │   │  ✓ Native image      │   │                       │
                  │  ✓ JaCoCo coverage  │   │  ✓ Docker push       │   │  → Production         │
                  │  ✓ SonarQube gate   │   │  ✓ Smoke test        │   │    (manual approval)  │
                  └─────────────────────┘   └──────────────────────┘   └───────────────────────┘
```

| Workflow     | Trigger         | Purpose                                        |
|--------------|-----------------|------------------------------------------------|
| `ci.yml`     | Every push + PR | Tests, coverage, SonarQube quality gate        |
| `build.yml`  | Merge to `main` | GraalVM native image → container registry      |
| `deploy.yml` | After build     | K8s rolling update, manual gate for production |

### GitHub Actions Secrets

Go to **Settings → Secrets and variables → Actions** and add:

| Secret                   | Description                                 |
|--------------------------|---------------------------------------------|
| `SONAR_TOKEN`            | SonarQube → Account → Security → Token      |
| `SONAR_HOST_URL`         | SonarQube server URL                        |
| `KUBE_CONFIG_STAGING`    | `cat ~/.kube/config \| base64` (staging)    |
| `KUBE_CONFIG_PRODUCTION` | `cat ~/.kube/config \| base64` (production) |
| `REGISTRY_URL`           | Container registry URL                      |

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

# Optional — only if running a self-hosted SonarQube in-cluster
kubectl apply -f hotel-backend/k8s/sonarqube.yaml
```

### Before you deploy

1. **Update the image tag** in `k8s/app.yaml`:
   ```
   image: your-registry.io/hotel-backend:1.0.0
   ```

2. **Rotate all secrets** — the defaults in `k8s/postgres.yaml` and `k8s/app.yaml` are base64-encoded placeholders. Never deploy them as-is:
   ```bash
   echo -n "your-real-password" | base64
   ```
   In production, prefer [Sealed Secrets](https://github.com/bitnami-labs/sealed-secrets) or [External Secrets Operator](https://external-secrets.io) over plain Kubernetes Secrets.

3. **Update the domain** in `k8s/ingress-and-netpol.yaml`:
   ```yaml
   host: api.hotel.yourdomain.com
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
kubectl logs -f deployment/postgres -n hotel
```

### Production resource profile (native image)

| Component     | CPU request | CPU limit | Memory request | Memory limit |
|---------------|-------------|-----------|----------------|--------------|
| hotel-backend | 100m        | 500m      | 64Mi           | 128Mi        |
| postgres      | 250m        | 1000m     | 256Mi          | 512Mi        |

The HPA scales the app between 2 and 10 replicas on CPU > 70% or memory > 80%. Native image cold start < 100ms makes scale-out near-instant.

---

---

## License

MIT
