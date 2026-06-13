package id.co.hospitops.hotel.adapter.web;

import id.co.hospitops.shared.web.BaseApiExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Exception → HTTP response mapping scoped to the hotel module's web adapter.
 *
 * <p>All handler logic lives in {@link BaseApiExceptionHandler}. The
 * {@code basePackageClasses} scope serves two purposes:
 * <ol>
 *   <li>Keeps {@code @WebMvcTest} slices working — they only load beans in this
 *       package and cannot see the bootstrap-level {@code GlobalExceptionHandler}.</li>
 *   <li>Prevents "Ambiguous @ExceptionHandler" startup failures in full-context
 *       ({@code @SpringBootTest}) tests: Spring MVC treats package-scoped advisors as
 *       non-overlapping with the unscoped global handler.</li>
 * </ol>
 */
@RestControllerAdvice(basePackageClasses = HotelExceptionHandler.class)
class HotelExceptionHandler extends BaseApiExceptionHandler {
}
