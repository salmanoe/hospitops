# ═══════════════════════════════════════════════════════════════
# Dockerfile — JVM fat-JAR (development & CI)
#
# Used by:  docker compose up --build
#           GitHub Actions ci.yml (smoke test)
#
# Production uses the GraalVM native image built in build.yml:
#   mvn -Pnative spring-boot:build-image -pl bootstrap -am
# ═══════════════════════════════════════════════════════════════

# ── Stage 1: Build ──────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /workspace

# Copy all POM files before the source tree so that Docker (BuildKit)
# can cache the dependency download layer independently of source changes.
# Rebuild only re-downloads when a pom.xml actually changes.
COPY pom.xml                    .
COPY shared/pom.xml             shared/pom.xml
COPY identity/pom.xml           identity/pom.xml
COPY room/pom.xml               room/pom.xml
COPY guest/pom.xml              guest/pom.xml
COPY reservation/pom.xml        reservation/pom.xml
COPY housekeeping/pom.xml       housekeeping/pom.xml
COPY billing/pom.xml            billing/pom.xml
COPY group/pom.xml              group/pom.xml
COPY hotel/pom.xml              hotel/pom.xml
COPY bootstrap/pom.xml          bootstrap/pom.xml
COPY coverage-aggregate/pom.xml coverage-aggregate/pom.xml

# Download all dependencies (cached unless a pom.xml changes)
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline --no-transfer-progress -q

# Copy the full source tree and build the fat JAR (tests skipped here;
# CI runs them in a dedicated step before the image build)
COPY . .
RUN --mount=type=cache,target=/root/.m2 \
    mvn package -pl bootstrap -am -Dmaven.test.skip=true --no-transfer-progress -q

# ── Stage 2: Run ─────────────────────────────────────────────────
FROM eclipse-temurin:25-jre

# Non-root user — GID/UID 10001 avoids the ubuntu:1000 that ships in
# eclipse-temurin base images. K8s securityContext uses the same value.
RUN groupadd --system --gid 10001 hotel \
 && useradd  --system --uid 10001 --gid 10001 hotel

WORKDIR /app

COPY --from=build --chown=hotel:hotel \
    /workspace/bootstrap/target/bootstrap-*.jar app.jar

USER hotel

EXPOSE 8080

ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
