package id.co.hospitops.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Bootstrap entry point.
 *
 * <p>Component scan starts here and covers every module on the classpath
 * (identity, guest, reservation, room, housekeeping, billing, shared).
 * Each module lives under {@code id.co.hospitops.*}, which is included
 * via the {@link SpringBootApplication#scanBasePackages} declaration.
 *
 * <p>{@link EnableJpaRepositories} must be declared explicitly here because
 * Spring Boot's JPA autoconfiguration defaults the repository scan root to
 * the package of the main class ({@code id.co.hospitops.bootstrap}), which
 * would miss all repositories declared in the domain module JARs.
 */
@SpringBootApplication(scanBasePackages = "id.co.hospitops")
@EnableJpaRepositories(basePackages = "id.co.hospitops")
@EntityScan(basePackages = "id.co.hospitops")
public class HospitOpsApplication {

    static void main(String[] args) {
        SpringApplication.run(HospitOpsApplication.class, args);
    }
}

