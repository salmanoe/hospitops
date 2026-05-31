package id.co.hospitops.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.co.hospitops.shared.HotelContext;
import id.co.hospitops.shared.web.ApiResponse;
import id.co.hospitops.shared.web.RequiresHotelContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * Enforces {@link RequiresHotelContext} on hotel-scoped controllers and methods.
 *
 * <p>If the annotation is present on the handler class or method and
 * {@link HotelContext#isBound()} is {@code false}, this interceptor short-circuits
 * the request with HTTP 403 before the controller method is invoked.
 *
 * <p>This prevents GROUP_ADMIN tokens (which carry no {@code hotelId} claim) from
 * accidentally reaching hotel-scoped endpoints and causing {@code IllegalStateException}
 * deep inside the repository layer.
 *
 * <p>The 403 response body is serialized through Jackson so it always matches the
 * current {@link ApiResponse} contract — including any fields added in the future.
 *
 * <p>Registered in {@link WebMvcConfig} for {@code /api/**}.
 */
@Component
@RequiredArgsConstructor
public class HotelContextHandlerInterceptor implements HandlerInterceptor {

    static final String MISSING_HOTEL_CONTEXT_MESSAGE =
            "A hotel-scoped token is required for this endpoint. "
            + "Obtain one via POST /api/v1/group/hotels/{hotelId}/enter.";

    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) throws IOException {

        if (!(handler instanceof HandlerMethod handlerMethod)) {
            // Non-method handlers (e.g. static resources) — let them pass.
            return true;
        }

        boolean requiresContext = handlerMethod.hasMethodAnnotation(RequiresHotelContext.class)
                || handlerMethod.getBeanType().isAnnotationPresent(RequiresHotelContext.class);

        if (requiresContext && !HotelContext.isBound()) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    objectMapper.writeValueAsString(ApiResponse.error(MISSING_HOTEL_CONTEXT_MESSAGE)));
            return false;
        }

        return true;
    }
}
