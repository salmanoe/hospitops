package id.co.hospitops.hotel.application;

import id.co.hospitops.hotel.application.command.CompleteSetupStepCommand;
import id.co.hospitops.hotel.application.command.CreateHotelCommand;
import id.co.hospitops.hotel.domain.model.HotelStatus;
import id.co.hospitops.hotel.application.response.HotelResponse;
import id.co.hospitops.hotel.domain.model.Hotel;
import id.co.hospitops.hotel.domain.port.in.ManageHotelUseCase;
import id.co.hospitops.hotel.domain.port.out.HotelRepository;
import id.co.hospitops.shared.GroupId;
import id.co.hospitops.shared.HotelId;
import id.co.hospitops.shared.event.HotelActivatedEvent;
import id.co.hospitops.shared.event.HotelCreatedEvent;
import id.co.hospitops.shared.event.HotelReactivatedEvent;
import id.co.hospitops.shared.event.HotelSuspendedEvent;
import id.co.hospitops.shared.exception.BusinessRuleViolationException;
import id.co.hospitops.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class HotelService implements ManageHotelUseCase {

    private final HotelRepository hotelRepo;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public HotelResponse createHotel(CreateHotelCommand cmd) {
        Hotel hotel = Hotel.create(cmd.groupId(), cmd.name());
        Hotel saved = hotelRepo.save(hotel);
        eventPublisher.publishEvent(new HotelCreatedEvent(saved.getId(), saved.getGroupId()));
        return HotelResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public HotelResponse findById(HotelId id, GroupId callerGroupId) {
        return HotelResponse.from(requireHotel(id, callerGroupId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<HotelResponse> findByGroupId(GroupId groupId) {
        return hotelRepo.findByGroupId(groupId)
                .stream().map(HotelResponse::from).toList();
    }

    /**
     * Marks a setup step as complete, trusting the GROUP_ADMIN to signal
     * when the underlying data is ready.
     *
     * <p><strong>Design trade-off (W4):</strong> this endpoint does <em>not</em>
     * verify that the prerequisite data actually exists (e.g. it does not check
     * {@code COUNT(*) FROM room_type WHERE hotel_id = ?} before accepting
     * {@code ROOM_TYPE}). The GROUP_ADMIN is trusted to signal steps accurately.
     *
     * <p>The risk is that a misconfigured hotel can reach ACTIVE status with zero
     * inventory. This was accepted as a deliberate simplification for the current
     * phase — the setup wizard UI is the primary guard, and the GROUP_ADMIN is an
     * authenticated, privileged actor with full responsibility for their hotels.
     *
     * <p><strong>Revisit when:</strong> self-service onboarding is introduced, or
     * when audit requirements demand proof that each step was data-backed.
     * At that point, add per-step repository count checks here before delegating
     * to {@code hotel.completeSetupStep}.
     */
    @Override
    public HotelResponse completeSetupStep(CompleteSetupStepCommand cmd) {
        Hotel hotel = requireHotel(cmd.hotelId(), cmd.callerGroupId());
        boolean justActivated = hotel.completeSetupStep(cmd.step());
        Hotel saved = hotelRepo.save(hotel);
        if (justActivated) {
            eventPublisher.publishEvent(new HotelActivatedEvent(saved.getId()));
        }
        return HotelResponse.from(saved);
    }

    @Override
    public HotelResponse suspend(HotelId id, GroupId callerGroupId) {
        Hotel hotel = requireHotel(id, callerGroupId);
        hotel.suspend();
        Hotel saved = hotelRepo.save(hotel);
        eventPublisher.publishEvent(new HotelSuspendedEvent(saved.getId()));
        return HotelResponse.from(saved);
    }

    @Override
    public HotelResponse reactivate(HotelId id, GroupId callerGroupId) {
        Hotel hotel = requireHotel(id, callerGroupId);
        hotel.reactivate();
        Hotel saved = hotelRepo.save(hotel);
        eventPublisher.publishEvent(new HotelReactivatedEvent(saved.getId()));
        return HotelResponse.from(saved);
    }

    @Override
    public void deleteHotel(HotelId id, GroupId callerGroupId) {
        Hotel hotel = requireHotel(id, callerGroupId);
        if (hotel.getStatus() != HotelStatus.SETUP) {
            throw new BusinessRuleViolationException(
                    "Only hotels in SETUP status can be deleted. " +
                    "Suspend the hotel first if you need to take it offline.");
        }
        hotelRepo.deleteById(id);
    }

    /**
     * Loads the hotel aggregate and verifies group ownership.
     *
     * @throws ResourceNotFoundException if no hotel exists with the given ID
     * @throws BusinessRuleViolationException if the hotel belongs to a different group —
     *         deliberately returns a generic message to avoid group enumeration
     */
    private Hotel requireHotel(HotelId id, GroupId callerGroupId) {
        Hotel hotel = hotelRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel", id.value()));
        if (!hotel.getGroupId().equals(callerGroupId)) {
            throw new BusinessRuleViolationException(
                    "Hotel does not belong to your group");
        }
        return hotel;
    }
}
