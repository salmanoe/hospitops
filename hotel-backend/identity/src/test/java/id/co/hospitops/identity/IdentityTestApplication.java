package id.co.hospitops.identity;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Minimal Spring Boot bootstrap for @WebMvcTest slices in the identity module.
 *
 * The real @SpringBootApplication lives in the bootstrap module, which is not
 * on the test classpath here.  @WebMvcTest requires a @SpringBootConfiguration
 * somewhere in the package hierarchy, so this class provides it.
 *
 * Spring Security autoconfiguration is explicitly excluded so that the
 * @WebMvcTest slice loads cleanly without requiring a SecurityFilterChain.
 * Controller-layer tests focus on HTTP semantics; security behaviour is
 * covered by integration tests that load the full application context.
 */
@SpringBootConfiguration
@EnableAutoConfiguration(exclude = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
})
@ComponentScan("id.co.hospitops.identity")
class IdentityTestApplication {
}
