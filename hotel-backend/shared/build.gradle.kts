// Shared kernel — value objects, typed IDs, domain events, API wrappers,
// BaseApiExceptionHandler. Stays THIN: no JPA, no business logic.
dependencies {
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-web")
    implementation("org.springframework:spring-webmvc")
    implementation("jakarta.validation:jakarta.validation-api")
    implementation("com.fasterxml.jackson.core:jackson-annotations")
}
