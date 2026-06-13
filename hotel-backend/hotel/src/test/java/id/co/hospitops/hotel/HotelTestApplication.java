package id.co.hospitops.hotel;

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
 * Minimal Spring Boot bootstrap for {@code @WebMvcTest} slices in the hotel module.
 *
 * <p>Security autoconfiguration is excluded and {@link AuthenticationPrincipalArgumentResolver}
 * is registered manually. See {@link id.co.hospitops.group.GroupTestApplication} for the
 * full rationale.
 */
@SpringBootConfiguration
@EnableAutoConfiguration(exclude = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
})
@ComponentScan("id.co.hospitops.hotel.adapter.web")
class HotelTestApplication {

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
