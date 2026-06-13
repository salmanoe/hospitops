import org.springframework.boot.gradle.tasks.aot.ProcessAot
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

// The ONLY deployable module — assembles every bounded context into the
// runnable Spring Boot application and the GraalVM native image.
plugins {
    id("org.springframework.boot")
    id("org.graalvm.buildtools.native")
}

dependencies {
    // ── Bounded-context modules ─────────────────────────────────────────
    implementation(project(":shared"))
    implementation(project(":group"))
    implementation(project(":hotel"))
    implementation(project(":identity"))
    implementation(project(":room"))
    implementation(project(":guest"))
    implementation(project(":reservation"))
    implementation(project(":housekeeping"))
    implementation(project(":billing"))
    implementation(project(":channel"))

    // ── Infrastructure ──────────────────────────────────────────────────
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    // AuditLoggingAspect (@Aspect/@Around) — aspectjweaver runtime only;
    // spring-aop arrives transitively via spring-context.
    implementation("org.aspectj:aspectjweaver")
    // Redis-backed token blacklist for multi-replica deployments.
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    // GroupTokenServiceAdapter uses java-jwt directly (was transitive via identity under Maven).
    implementation("com.auth0:java-jwt:4.5.2")

    // Database + migrations.
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    // Registers FlywayAutoConfiguration (split out in Spring Boot 4.0).
    implementation("org.springframework.boot:spring-boot-flyway")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // ── Test ────────────────────────────────────────────────────────────
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-test-autoconfigure")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.springframework.security:spring-security-test")
    // Versions forced to 1.21.4 via the root resolutionStrategy.
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:junit-jupiter")
}

springBoot {
    mainClass.set("id.co.hospitops.bootstrap.HospitOpsApplication")
}

// AOT introspection spins up a partial context with no DB available —
// disable Flyway + schema validation for that phase only (runtime is unaffected).
tasks.named<ProcessAot>("processAot") {
    jvmArgs(
        "-Dspring.jpa.hibernate.ddl-auto=none",
        "-Dspring.jpa.properties.hibernate.hbm2ddl.auto=none",
        "-Dspring.flyway.enabled=false",
        "-Dspring.datasource.url=jdbc:postgresql://localhost:5432/hotel_db_aot_placeholder",
        "-Dspring.datasource.username=placeholder",
        "-Dspring.datasource.password=placeholder",
    )
}

// Production native container via Paketo buildpacks (compilation happens inside
// the build container — no local native-image needed). Image name is supplied on
// the CLI: ./gradlew :bootstrap:bootBuildImage --imageName=ghcr.io/<owner>/hotel-backend:<tag>
tasks.named<BootBuildImage>("bootBuildImage") {
    environment.set(
        mapOf(
            "BP_NATIVE_IMAGE" to "true",
            "BP_JVM_VERSION" to "25",
        )
    )
}

graalvmNative {
    metadataRepository { enabled.set(true) }
    binaries {
        named("main") {
            imageName.set("hospitops-backend")
            mainClass.set("id.co.hospitops.bootstrap.HospitOpsApplication")
            // Build a standalone executable, not a shared library.
            sharedLibrary.set(false)
            buildArgs.add("--no-fallback")
            buildArgs.add("-march=compatibility")
            buildArgs.add("-H:+ReportExceptionStackTraces")
            // NOTE: the Maven POM also forced `--initialize-at-build-time=org.slf4j`,
            // but that pulls Logback's run-time-initialized LogbackMDCAdapter into the
            // build-time image heap and fails the build. Spring Boot's native hints
            // already initialize slf4j/logback correctly, so the arg is dropped here.
            buildArgs.add("--initialize-at-run-time=org.hibernate.engine.jdbc.dialect.internal")
        }
    }
}
