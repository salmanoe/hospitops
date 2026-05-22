package id.co.hospitops.bootstrap;

// R3-06 FIX: Closes the audit gap left by AuditLoggingAspect.
//
// AuditLoggingAspect uses an @Around advice on controller method join points.
// When Spring MVC's argument resolution rejects a request body via @Valid
// (MethodArgumentNotValidException), the exception is thrown BEFORE the join
// point is entered, so the aspect never fires. As a result, malformed POST /
// PATCH / PUT / DELETE requests are not audited at all.
//
// This HandlerInterceptor fires preHandle() for every request, before argument
// binding and validation. For mutating methods (POST, PATCH, PUT, DELETE) it
// logs the actor + URI so there is always a trace entry — regardless of whether
// validation passes or fails. The AuditLoggingAspect then adds a SUCCESS/FAILED
// entry once the method completes; together they give a complete picture.
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

@Slf4j
@Component
@NullMarked
public class MutatingRequestAuditInterceptor implements HandlerInterceptor {

    private static final Set<String> MUTATING_METHODS =
            Set.of("POST", "PUT", "PATCH", "DELETE");

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             @Nullable Object handler) {
        if (MUTATING_METHODS.contains(request.getMethod())) {
            log.info("[AUDIT] {} | {} {} | RECEIVED",
                     currentUser(), request.getMethod(), request.getRequestURI());
        }
        return true; // always continue processing
    }

    private String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "anonymous";
    }
}
