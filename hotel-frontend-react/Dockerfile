# ═══════════════════════════════════════════════════════════════
# Dockerfile — React SPA (Vite build → nginx)
#
# Used by:  docker compose up --build   (frontend service)
#           production deploy / k8s
#
# Stage 1 builds the static bundle with Node; stage 2 serves it with
# nginx and proxies /api/* to the Spring Boot backend.
# ═══════════════════════════════════════════════════════════════

# ── Stage 1: Build ──────────────────────────────────────────────
FROM node:22-alpine AS build

WORKDIR /app

# Install deps against the lockfile first so this layer is cached
# independently of source changes.
COPY package.json package-lock.json ./
RUN npm ci

# Build the production bundle (tsc type-check + vite build → dist/).
COPY . .
RUN npm run build

# ── Stage 2: Serve ──────────────────────────────────────────────
FROM nginx:alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
