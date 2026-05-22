package id.co.hospitops.bootstrap;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * AOP audit logging — scoped to hotel web adapters only (R2 fix).
 */
@Slf4j
@Aspect
@Component
public class AuditLoggingAspect {

    @SuppressWarnings("unused") // invoked by AspectJ weaver, not called directly
    @Around("(execution(* id.co.hospitops.*.adapter.web..*(..)))"
            + " && (@annotation(org.springframework.web.bind.annotation.PostMapping)"
            + " || @annotation(org.springframework.web.bind.annotation.PatchMapping)"
            + " || @annotation(org.springframework.web.bind.annotation.PutMapping)"
            + " || @annotation(org.springframework.web.bind.annotation.DeleteMapping))")
    public Object auditStateChange(ProceedingJoinPoint joinPoint) throws Throwable {
        String actor = currentUser();
        String method = joinPoint.getSignature().toShortString();
        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            log.info("[AUDIT] {} | {} | SUCCESS | {}ms", actor, method, System.currentTimeMillis() - start);
            return result;
        } catch (Exception ex) {
            log.warn("[AUDIT] {} | {} | FAILED: {} | {}ms", actor, method, ex.getMessage(), System.currentTimeMillis() - start);
            throw ex;
        }
    }

    private String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "anonymous";
    }
}
