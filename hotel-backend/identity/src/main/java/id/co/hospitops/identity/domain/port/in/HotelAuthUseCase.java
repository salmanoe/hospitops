package id.co.hospitops.identity.domain.port.in;

import id.co.hospitops.identity.application.command.HotelLoginCommand;
import id.co.hospitops.identity.application.response.LoginResponse;

/**
 * Inbound port — hotel-aware staff authentication.
 *
 * <p>Validates that the hotel is ACTIVE before attempting credential verification,
 * and ensures the staff account belongs to the requested hotel.
 * Contrast with {@link AuthUseCase#login} which is the legacy unscoped endpoint.
 */
public interface HotelAuthUseCase {

    /**
     * Authenticates a staff member against a specific hotel.
     *
     * @throws id.co.hospitops.shared.exception.BusinessRuleViolationException if the hotel
     *                                                                         is not ACTIVE, credentials are invalid, or the staff does not belong to
     *                                                                         the requested hotel
     */
    LoginResponse login(HotelLoginCommand command);
}
