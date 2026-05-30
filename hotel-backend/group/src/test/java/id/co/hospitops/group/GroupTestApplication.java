package id.co.hospitops.group;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Minimal Spring Boot bootstrap for {@code @WebMvcTest} slices in the group module.
 *
 * <p>The real {@code @SpringBootApplication} lives in {@code bootstrap} and is not
 * on this module's test classpath. Spring Security autoconfiguration is excluded so
 * controller-layer tests can focus on HTTP semantics; security is covered by
 * integration tests that load the full application context.
 *
 * <p>The {@code @ComponentScan} is intentionally scoped to the web adapter package only.
 * Scanning the whole {@code id.co.hospitops.group} package pulls in {@code @Repository}
 * and {@code @Service} beans, which fail to load without a JPA datasource in a web
 * test slice. Limiting the scan to {@code adapter.web} loads only controllers, which is
 * exactly what {@code @WebMvcTest} needs.
 */
@SpringBootConfiguration
@EnableAutoConfiguration(exclude = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
})
@ComponentScan("id.co.hospitops.group.adapter.web")
class GroupTestApplication {
}
