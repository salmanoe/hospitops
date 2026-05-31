package id.co.hospitops.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Explicit ObjectMapper bean for the bootstrap module.
 *
 * <p>Spring Boot's JacksonAutoConfiguration creates an ObjectMapper when
 * spring-boot-starter-json is on the classpath, but declaring it explicitly
 * here makes the dependency visible, avoids autoconfiguration ordering surprises,
 * and guarantees the bean is available for {@link HotelContextHandlerInterceptor}
 * regardless of which starters are present.
 *
 * <p>{@link JavaTimeModule} is registered directly (rather than via
 * {@code findAndRegisterModules}) to avoid relying on SPI classpath discovery,
 * which is unreliable in modular test environments.
 *
 * <p>{@link ConditionalOnMissingBean} ensures this definition backs off if a
 * customized ObjectMapper is declared elsewhere (e.g. in a test configuration).
 */
@Configuration
public class JacksonConfig {

    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
