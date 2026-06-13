package id.co.hospitops.identity;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Minimal Spring Boot bootstrap for @WebMvcTest slices in the identity module.
 * <p>
 * The real @SpringBootApplication lives in the bootstrap module, which is not
 * on the test classpath here.  @WebMvcTest requires a @SpringBootConfiguration
 * somewhere in the package hierarchy, so this class provides it.
 * <p>
 * Spring Security autoconfiguration is explicitly excluded so that the
 *
 * @WebMvcTest slice loads cleanly without requiring a SecurityFilterChain.
 * Controller-layer tests focus on HTTP semantics; security behavior is
 * covered by integration tests that load the full application context.
 */
@SpringBootConfiguration
@EnableAutoConfiguration(exclude = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
})
// Scoped to the web adapter only — scanning all id.co.hospitops.identity pulls in
// JpaRepository and infrastructure beans that fail without a datasource in a web slice.
// @WebMvcTest's type filter then limits what's loaded to controllers and @ControllerAdvice.
@ComponentScan("id.co.hospitops.identity.adapter.web")
class IdentityTestApplication {
}
