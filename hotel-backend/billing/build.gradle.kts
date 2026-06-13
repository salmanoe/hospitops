dependencies {
    api(project(":shared"))
    // ReservationDetailAdapter assembles invoice detail from these modules.
    // Stage 3: replace with HTTP clients and drop these.
    api(project(":reservation"))
    api(project(":guest"))
    api(project(":room"))
    // BillingController uses Staff as @AuthenticationPrincipal.
    api(project(":identity"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    // JdbcTemplate for InvoiceNumberGeneratorImpl.
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    // Controller uses @AuthenticationPrincipal (was transitive via identity under Maven).
    implementation("org.springframework.boot:spring-boot-starter-security")

    // iText 9 for invoice PDF generation (pom-packaged aggregator artifact).
    implementation("com.itextpdf:itext-core:9.6.0")
}
