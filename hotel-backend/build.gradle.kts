import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoReport

// ═══════════════════════════════════════════════════════════════════════════
// HospitOps backend — Gradle Kotlin DSL build (migrated from Maven).
//
// Faithful to the previous Maven multi-module build:
//   • Java 25 LTS toolchain · Spring Boot 4.0.6 BOM · GraalVM native image
//   • Annotation-processor order: Lombok → lombok-mapstruct-binding →
//     MapStruct → hibernate-processor  (order is load-bearing — do not reorder)
//   • Testcontainers pinned to 1.21.4 (Spring Boot BOM points at 2.0.5, which
//     is not published to Maven Central) and commons-compress to 1.28.0 (CVE).
// ═══════════════════════════════════════════════════════════════════════════

plugins {
    java
    id("org.springframework.boot") version "4.0.6" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("org.graalvm.buildtools.native") version "1.1.2" apply false
    id("org.sonarqube") version "5.1.0.4882"
}

// ── Pinned dependency versions (single source of truth) ─────────────────────
val mapstructVersion = "1.6.3"
val lombokVersion = "1.18.46"
val lombokMapstructBindingVersion = "0.2.0"
val hibernateProcessorVersion = "7.3.6.Final"
val testcontainersVersion = "1.21.4"
val commonsCompressVersion = "1.28.0"
val jacocoVersion = "0.8.14"

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "jacoco")

    group = "id.co.hospitops"
    version = "1.0.0-SNAPSHOT"

    repositories { mavenCentral() }

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
    }

    // Import the Spring Boot BOM so Spring-managed artifacts need no versions.
    configure<DependencyManagementExtension> {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:4.0.6")
        }
    }

    // Hard overrides that the BOM gets wrong or doesn't cover.
    configurations.all {
        resolutionStrategy.eachDependency {
            when {
                requested.group == "org.testcontainers" ->
                    useVersion(testcontainersVersion)
                requested.group == "org.apache.commons" && requested.name == "commons-compress" ->
                    useVersion(commonsCompressVersion)
                requested.group == "org.projectlombok" && requested.name == "lombok" ->
                    useVersion(lombokVersion)
            }
        }
    }

    dependencies {
        // Lombok + MapStruct + Hibernate metamodel — processor ORDER matters.
        "compileOnly"("org.projectlombok:lombok:$lombokVersion")
        "annotationProcessor"("org.projectlombok:lombok:$lombokVersion")
        "annotationProcessor"("org.projectlombok:lombok-mapstruct-binding:$lombokMapstructBindingVersion")
        "implementation"("org.mapstruct:mapstruct:$mapstructVersion")
        "annotationProcessor"("org.mapstruct:mapstruct-processor:$mapstructVersion")
        "annotationProcessor"("org.hibernate.orm:hibernate-processor:$hibernateProcessorVersion")

        "testCompileOnly"("org.projectlombok:lombok:$lombokVersion")
        "testAnnotationProcessor"("org.projectlombok:lombok:$lombokVersion")
        "testImplementation"("org.springframework.boot:spring-boot-starter-test")
        // Gradle 9 no longer bundles the JUnit Platform launcher on the test
        // runtime classpath — it must be declared explicitly (version via BOM).
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<JavaCompile>().configureEach {
        // -parameters: retain method parameter names so Spring MVC can infer
        // @PathVariable/@RequestParam names without explicit value=. The Spring
        // Boot Gradle plugin sets this, but only `bootstrap` applies that plugin —
        // the library modules need it set here. (Maven got it from the parent POM.)
        options.compilerArgs.addAll(
            listOf(
                "-parameters",
                "-Amapstruct.defaultComponentModel=spring",
                "-Amapstruct.unmappedTargetPolicy=ERROR",
            )
        )
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        // Deep-reflection opens previously set on maven-surefire-plugin.
        jvmArgs(
            "--add-opens=java.base/java.lang=ALL-UNNAMED",
            "--add-opens=java.base/java.util=ALL-UNNAMED",
            "--add-opens=java.base/java.time=ALL-UNNAMED",
        )
    }

    // JaCoCo — always produce XML so SonarQube can read per-module coverage.
    configure<JacocoPluginExtension> { toolVersion = jacocoVersion }
    tasks.withType<JacocoReport>().configureEach {
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }
    tasks.named("test") { finalizedBy("jacocoTestReport") }
}

// ── SonarQube (mirrors the Maven sonar profile/properties) ──────────────────
sonar {
    properties {
        property("sonar.projectKey", "hospitops-backend")
        property("sonar.projectName", "HospitOps Backend")
        property("sonar.java.source", "25")
        property("sonar.scanner.skipJreProvisioning", "true")
        property("sonar.qualitygate.wait", "true")
        property(
            "sonar.coverage.jacoco.xmlReportPaths",
            "${rootDir}/**/build/reports/jacoco/test/jacocoTestReport.xml",
        )
        property(
            "sonar.exclusions",
            listOf(
                "**/infrastructure/persistence/entity/**",
                "**/adapter/web/request/**",
                "**/adapter/web/response/**",
                "**/*JpaEntity.java",
                "**/*Mapper.java",
                "**/HospitOpsApplication.java",
                "**/shared/event/**",
            ).joinToString(","),
        )
        property(
            "sonar.coverage.exclusions",
            listOf(
                "**/infrastructure/persistence/entity/**",
                "**/*JpaEntity.java",
                "**/*Mapper.java",
                "**/HospitOpsApplication.java",
                "**/config/**",
                "**/shared/event/**",
            ).joinToString(","),
        )
    }
}
