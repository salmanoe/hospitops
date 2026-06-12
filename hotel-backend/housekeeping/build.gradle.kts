dependencies {
    api(project(":shared"))
    // RoomStatusAdapter calls the room module application layer.
    // Stage 3: replace with HTTP client and drop this.
    api(project(":room"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
}
