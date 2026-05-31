package id.co.hospitops.bootstrap.web;

import id.co.hospitops.shared.web.ApiResponse;
import id.co.hospitops.shared.web.BaseApiExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Application-wide exception → HTTP response mapping.
 *
 * <p>Extends {@link BaseApiExceptionHandler} which handles the common domain
 * exceptions (404, 409, 422, 400). This class adds catch-alls that are
 * intentionally absent from the base: {@code IllegalStateException} (422)
 * and the generic {@code Exception} fallback (500), which should never fire
 * in production but guard against unexpected leakage.
 *
 * <p>Each module also has a thin subclass of {@link BaseApiExceptionHandler}
 * so that {@code @WebMvcTest} slices — which can't see this bean — still map
 * exceptions correctly.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends BaseApiExceptionHandler {

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ApiResponse<Void> handleIllegalState(IllegalStateException ex) {
        return ApiResponse.error(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleGeneric(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return ApiResponse.error("An unexpected error occurred");
    }
}
