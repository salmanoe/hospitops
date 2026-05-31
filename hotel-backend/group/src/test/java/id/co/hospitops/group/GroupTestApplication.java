package id.co.hospitops.group;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Minimal Spring Boot bootstrap for {@code @WebMvcTest} slices in the group module.
 *
 * <p>The real {@code @SpringBootApplication} lives in {@code bootstrap} and is not
 * on this module's test classpath. The {@code @ComponentScan} is intentionally scoped
 * to {@code adapter.web} only to avoid pulling in {@code @Repository} and
 * {@code @Service} beans that require a JPA datasource.
 *
 * <p>Security autoconfiguration is excluded to keep the web-slice context lightweight.
 * Bringing {@code SecurityAutoConfiguration} back pulls in {@code UserDetailsServiceAutoConfiguration}
 * and other beans that cannot be satisfied without a full application context.
 *
 * <p>Instead, {@link AuthenticationPrincipalArgumentResolver} is registered manually via
 * a {@link WebMvcConfigurer} bean. Tests that need a principal set {@link
 * org.springframework.security.core.context.SecurityContextHolder} directly in
 * {@code @BeforeEach} — this works because the resolver reads from
 * {@code SecurityContextHolder}, not from the security filter chain.
 */
@SpringBootConfiguration
@EnableAutoConfiguration(exclude = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
})
@ComponentScan("id.co.hospitops.group.adapter.web")
class GroupTestApplication {

    /**
     * Registers {@link AuthenticationPrincipalArgumentResolver} so that
     * {@code @AuthenticationPrincipal} controller parameters resolve correctly
     * without requiring the full Spring Security autoconfiguration.
     */
    @Bean
    WebMvcConfigurer authPrincipalArgumentResolverConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
                resolvers.add(new AuthenticationPrincipalArgumentResolver());
            }
        };
    }
}
