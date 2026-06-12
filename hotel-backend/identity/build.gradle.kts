dependencies {
    api(project(":shared"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // JWT access/refresh tokens.
    implementation("com.auth0:java-jwt:4.5.2")

    // Redis-backed token blacklist + refresh-token store (conditional beans,
    // but the types must resolve at compile time regardless of active profile).
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    testImplementation("org.springframework.boot:spring-boot-webmvc-test")
}
