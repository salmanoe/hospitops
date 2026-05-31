package id.co.hospitops.shared.web;

import id.co.hospitops.shared.exception.BusinessRuleViolationException;
import id.co.hospitops.shared.exception.ConflictException;
import id.co.hospitops.shared.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.stream.Collectors;

/**
 * Base exception-to-HTTP-response mapping shared across all module-level
 * {@code @RestControllerAdvice} beans.
 *
 * <p>Each capability module (group, hotel, identity, …) has a thin concrete subclass
 * annotated with {@code @RestControllerAdvice}. This design exists because
 * {@code @WebMvcTest} slices only load beans within the tested module's package —
 * the bootstrap-level {@code GlobalExceptionHandler} is not visible to those slices.
 * Subclassing rather than copying keeps the handler logic in one place.
 *
 * <p>{@code GlobalExceptionHandler} in {@code bootstrap} extends this class to cover
 * full-context integration tests and production runtime.
 */
public abstract class BaseApiExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNotFound(ResourceNotFoundException ex) {
        return ApiResponse.error(ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Void> handleConflict(ConflictException ex) {
        return ApiResponse.error(ex.getMessage());
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ApiResponse<Void> handleBusinessRule(BusinessRuleViolationException ex) {
        return ApiResponse.error(ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ApiResponse.error(message);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleIllegalArg(IllegalArgumentException ex) {
        return ApiResponse.error(ex.getMessage());
    }
}
