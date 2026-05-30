package id.co.hospitops.group.domain.port.in;

import id.co.hospitops.group.application.command.GroupLoginCommand;
import id.co.hospitops.group.application.response.GroupLoginResponse;
import id.co.hospitops.shared.GroupAdminPrincipal;
import id.co.hospitops.shared.HotelId;

/**
 * Inbound port — GROUP_ADMIN authentication and hotel context switching.
 */
public interface GroupAuthUseCase {

    /**
     * Authenticates a GROUP_ADMIN and issues a group-scoped JWT (no hotelId claim).
     *
     * @throws id.co.hospitops.shared.exception.BusinessRuleViolationException on bad credentials
     */
    GroupLoginResponse login(GroupLoginCommand command);

    /**
     * Exchanges the caller's group-scoped token for a hotel-scoped token.
     *
     * <p>Security rules enforced:
     * <ol>
     *   <li>The hotel must belong to the caller's group.</li>
     *   <li>The hotel must be in {@code ACTIVE} status.</li>
     *   <li>The issued hotel token has the <em>same expiry</em> as the originating group token.</li>
     * </ol>
     *
     * @param admin       the GROUP_ADMIN principal extracted from the group-scoped JWT
     * @param targetHotel the hotel to enter
     * @param groupToken  the raw group-scoped JWT string (used to extract expiry)
     * @throws id.co.hospitops.shared.exception.BusinessRuleViolationException if the hotel
     *                                                                         does not belong to the group or is not ACTIVE
     */
    GroupLoginResponse enterHotel(GroupAdminPrincipal admin, HotelId targetHotel, String groupToken);
}
