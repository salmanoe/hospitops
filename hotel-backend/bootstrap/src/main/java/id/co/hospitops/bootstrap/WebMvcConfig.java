package id.co.hospitops.bootstrap;

// Registers MutatingRequestAuditInterceptor so it fires for every API request.
// Scoped to /api/** only — actuator and other paths are excluded.

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final MutatingRequestAuditInterceptor auditInterceptor;
    private final HotelContextHandlerInterceptor hotelContextInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Hotel-context enforcement runs first so 403 is returned before the audit log fires.
        registry.addInterceptor(hotelContextInterceptor)
                .addPathPatterns("/api/**");
        registry.addInterceptor(auditInterceptor)
                .addPathPatterns("/api/**");
    }
}
