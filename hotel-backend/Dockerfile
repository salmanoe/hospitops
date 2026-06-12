# ═══════════════════════════════════════════════════════════════
# Dockerfile — JVM fat-JAR (development & CI)
#
# Used by:  docker compose up --build
#           GitHub Actions ci.yml (smoke test)
#
# Production uses the GraalVM native image built in build.yml:
#   ./gradlew :bootstrap:bootBuildImage   (or :bootstrap:nativeCompile)
# ═══════════════════════════════════════════════════════════════

# ── Stage 1: Build ──────────────────────────────────────────────
FROM eclipse-temurin:25-jdk AS build

WORKDIR /workspace

# Copy the Gradle wrapper + build scripts before the source tree so the
# dependency-resolution layer is cached independently of source changes.
# Rebuild only re-resolves when a build script changes.
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle/ gradle/
COPY shared/build.gradle.kts        shared/build.gradle.kts
COPY group/build.gradle.kts         group/build.gradle.kts
COPY hotel/build.gradle.kts         hotel/build.gradle.kts
COPY identity/build.gradle.kts      identity/build.gradle.kts
COPY room/build.gradle.kts          room/build.gradle.kts
COPY guest/build.gradle.kts         guest/build.gradle.kts
COPY reservation/build.gradle.kts   reservation/build.gradle.kts
COPY housekeeping/build.gradle.kts  housekeeping/build.gradle.kts
COPY billing/build.gradle.kts       billing/build.gradle.kts
COPY bootstrap/build.gradle.kts     bootstrap/build.gradle.kts

# Warm the dependency cache (best-effort — succeeds even before sources exist).
RUN chmod +x gradlew && \
    ./gradlew --no-daemon -q :bootstrap:dependencies > /dev/null 2>&1 || true

# Copy the full source tree and build the fat JAR (tests run in a dedicated
# CI step before the image build, so they are skipped here).
COPY . .
RUN ./gradlew --no-daemon :bootstrap:bootJar -x test

# ── Stage 2: Run ─────────────────────────────────────────────────
FROM eclipse-temurin:25-jre

# curl — the eclipse-temurin (Ubuntu) base ships no http client, so the
# compose / Docker HEALTHCHECK needs one to poll the readiness probe.
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/*

# Non-root user — GID/UID 10001 avoids the ubuntu:1000 that ships in
# eclipse-temurin base images. K8s securityContext uses the same value.
RUN groupadd --system --gid 10001 hotel \
 && useradd  --system --uid 10001 --gid 10001 hotel

WORKDIR /app

COPY --from=build --chown=hotel:hotel \
    /workspace/bootstrap/build/libs/bootstrap-*.jar app.jar

USER hotel

EXPOSE 8080

ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
