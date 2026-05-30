package id.co.hospitops.shared.web;

import java.lang.annotation.*;

/**
 * Marks a controller class or handler method as requiring a bound {@code HotelContext}.
 *
 * <p>When present, the {@code HotelContextHandlerInterceptor} in {@code bootstrap}
 * checks {@link id.co.hospitops.shared.HotelContext#isBound()} before the handler
 * is invoked. If {@code HotelContext} is not bound — for example, because the caller
 * sent a GROUP_ADMIN token with no {@code hotelId} claim — the interceptor immediately
 * returns HTTP 403 with a structured error body.
 *
 * <p>Apply at the class level to cover all methods in a hotel-scoped controller:
 * <pre>
 *   {@literal @}RequiresHotelContext
 *   {@literal @}RestController
 *   public class RoomController { ... }
 * </pre>
 *
 * <p>Apply at the method level for controllers that mix hotel-scoped and unscoped endpoints
 * (e.g., {@code IdentityController} which serves both auth and staff management):
 * <pre>
 *   {@literal @}GetMapping("/staff")
 *   {@literal @}RequiresHotelContext
 *   public ResponseEntity<?> listStaff(...) { ... }
 * </pre>
 *
 * @see id.co.hospitops.shared.HotelContext
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresHotelContext {
}
