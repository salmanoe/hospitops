package id.co.hospitops.hotel.domain.port.in;

import id.co.hospitops.hotel.application.command.CreateHotelCommand;
import id.co.hospitops.hotel.application.command.CompleteSetupStepCommand;
import id.co.hospitops.hotel.application.response.HotelResponse;
import id.co.hospitops.hotel.domain.model.SetupStep;
import id.co.hospitops.shared.GroupId;
import id.co.hospitops.shared.HotelId;

import java.util.List;

public interface ManageHotelUseCase {

    HotelResponse createHotel(CreateHotelCommand cmd);

    HotelResponse findById(HotelId id);

    List<HotelResponse> findByGroupId(GroupId groupId);

    /**
     * Marks a setup step complete. Returns the updated hotel.
     * If the checklist becomes complete, the hotel auto-transitions to ACTIVE
     * and a {@link id.co.hospitops.shared.event.HotelActivatedEvent} is published.
     */
    HotelResponse completeSetupStep(CompleteSetupStepCommand cmd);

    HotelResponse suspend(HotelId id);

    HotelResponse reactivate(HotelId id);
}
