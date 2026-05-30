package id.co.hospitops.group.adapter.web;

import id.co.hospitops.shared.exception.BusinessRuleViolationException;
import id.co.hospitops.shared.exception.ConflictException;
import id.co.hospitops.shared.exception.ResourceNotFoundException;
import id.co.hospitops.shared.web.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Exception → HTTP response mapping for the group module's web adapter.
 *
 * <p>Handles domain exceptions thrown by {@link id.co.hospitops.group.domain.port.in.ManageGroupUseCase}
 * and {@link id.co.hospitops.group.domain.port.in.GroupAuthUseCase} so that each maps
 * to a semantically correct HTTP status and a consistent {@link ApiResponse} envelope.
 *
 * <p>The bootstrap module's {@code GlobalExceptionHandler} remains the application-wide
 * fallback for any exceptions not handled here.
 */
@RestControllerAdvice
class GroupExceptionHandler {

    @ExceptionHandler(BusinessRuleViolationException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    public ApiResponse<Void> handleBusinessRule(BusinessRuleViolationException ex) {
        return ApiResponse.error(ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Void> handleConflict(ConflictException ex) {
        return ApiResponse.error(ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNotFound(ResourceNotFoundException ex) {
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
}
