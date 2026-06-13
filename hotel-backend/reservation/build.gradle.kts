dependencies {
    api(project(":shared"))
    // Cross-module application calls (room availability, guest validation).
    // Stage 3: replace with HTTP clients and drop these.
    api(project(":room"))
    api(project(":guest"))
    // Controller uses Staff as @AuthenticationPrincipal.
    api(project(":identity"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    // JdbcTemplate for ReservationNumberGeneratorImpl.
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    // Controller uses @AuthenticationPrincipal (was transitive via identity under Maven).
    implementation("org.springframework.boot:spring-boot-starter-security")
}
