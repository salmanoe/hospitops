pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "hotel-backend"

// One deployable module (bootstrap) + bounded-context libraries.
// NOTE: the Maven `coverage-aggregate` module is intentionally not ported —
// under Gradle, the SonarQube plugin consumes each module's own JaCoCo XML
// report directly (see root build.gradle.kts sonar config).
include(
    "shared",
    "group",
    "hotel",
    "identity",
    "room",
    "guest",
    "reservation",
    "housekeeping",
    "billing",
    "channel",
    "bootstrap",
)
