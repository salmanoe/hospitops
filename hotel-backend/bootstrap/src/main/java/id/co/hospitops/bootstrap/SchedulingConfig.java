package id.co.hospitops.bootstrap;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * R-10 FIX: Dedicated configuration class for Spring's @Scheduled support.
 *
 * <p>Previously {@code @EnableScheduling} was placed on {@link SecurityConfig},
 * giving that class a second reason to change (security policy + scheduling
 * lifecycle). This class is the sole owner of the scheduling concern.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
