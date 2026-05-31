package id.co.hospitops.hotel.domain.port.in;

import id.co.hospitops.hotel.application.command.CreateHotelCommand;
import id.co.hospitops.hotel.application.command.CompleteSetupStepCommand;
import id.co.hospitops.hotel.application.response.HotelResponse;
import id.co.hospitops.shared.GroupId;
import id.co.hospitops.shared.HotelId;

import java.util.List;

public interface ManageHotelUseCase {

    HotelResponse createHotel(CreateHotelCommand cmd);

    /**
     * Returns a hotel by ID, verifying it belongs to {@code callerGroupId}.
     *
     * @throws id.co.hospitops.shared.exception.ResourceNotFoundException if the hotel does not exist
     * @throws id.co.hospitops.shared.exception.BusinessRuleViolationException if the hotel belongs
     *         to a different group — prevents cross-group information disclosure
     */
    HotelResponse findById(HotelId id, GroupId callerGroupId);

    List<HotelResponse> findByGroupId(GroupId groupId);

    /**
     * Marks a setup step complete. Returns the updated hotel.
     * If the checklist becomes complete, the hotel auto-transitions to ACTIVE
     * and a {@link id.co.hospitops.shared.event.HotelActivatedEvent} is published.
     *
     * <p>{@code cmd.callerGroupId()} is verified against the hotel's group before the
     * operation is allowed — prevents GROUP_ADMIN from completing steps on another group's hotel.
     */
    HotelResponse completeSetupStep(CompleteSetupStepCommand cmd);

    /**
     * Suspends the hotel. Verifies {@code callerGroupId} owns the hotel.
     *
     * @throws id.co.hospitops.shared.exception.BusinessRuleViolationException if the hotel
     *         belongs to a different group or is not currently ACTIVE
     */
    HotelResponse suspend(HotelId id, GroupId callerGroupId);

    /**
     * Reactivates the hotel. Verifies {@code callerGroupId} owns the hotel.
     *
     * @throws id.co.hospitops.shared.exception.BusinessRuleViolationException if the hotel
     *         belongs to a different group or is not currently SUSPENDED
     */
    HotelResponse reactivate(HotelId id, GroupId callerGroupId);
}
