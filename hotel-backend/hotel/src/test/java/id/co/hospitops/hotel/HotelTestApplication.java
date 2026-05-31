package id.co.hospitops.hotel;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Minimal Spring Boot bootstrap for {@code @WebMvcTest} slices in the hotel module.
 *
 * <p>The real {@code @SpringBootApplication} lives in {@code bootstrap} and is not
 * on this module's test classpath. The scan is scoped to {@code adapter.web} only
 * to avoid pulling in {@code @Repository} and {@code @Service} beans that require
 * a JPA datasource.
 *
 * <p>{@code @WebMvcTest} applies its own type filter on top of this scan — only the
 * controller named in {@code @WebMvcTest(...)} is registered. Sibling controllers
 * in the same package (e.g. {@code GroupDashboardController}) are visible to the
 * scan but their use-case dependencies must be provided as {@code @MockitoBean}s in
 * each test class, exactly as {@code GroupAuthControllerTest} does for
 * {@code ManageGroupUseCase}.
 *
 * <p>Security autoconfiguration is absent from the classpath (no
 * {@code spring-boot-starter-security} dependency), so no excludes are needed.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan("id.co.hospitops.hotel.adapter.web")
class HotelTestApplication {
}
