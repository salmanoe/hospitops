package id.co.hospitops.hotel.application;

import id.co.hospitops.hotel.application.command.CompleteSetupStepCommand;
import id.co.hospitops.hotel.application.command.CreateHotelCommand;
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
    public HotelResponse findById(HotelId id) {
        return HotelResponse.from(requireHotel(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<HotelResponse> findByGroupId(GroupId groupId) {
        return hotelRepo.findByGroupId(groupId)
                .stream().map(HotelResponse::from).toList();
    }

    @Override
    public HotelResponse completeSetupStep(CompleteSetupStepCommand cmd) {
        Hotel hotel = requireHotel(cmd.hotelId());
        boolean justActivated = hotel.completeSetupStep(cmd.step());
        Hotel saved = hotelRepo.save(hotel);
        if (justActivated) {
            eventPublisher.publishEvent(new HotelActivatedEvent(saved.getId()));
        }
        return HotelResponse.from(saved);
    }

    @Override
    public HotelResponse suspend(HotelId id) {
        Hotel hotel = requireHotel(id);
        hotel.suspend();
        Hotel saved = hotelRepo.save(hotel);
        eventPublisher.publishEvent(new HotelSuspendedEvent(saved.getId()));
        return HotelResponse.from(saved);
    }

    @Override
    public HotelResponse reactivate(HotelId id) {
        Hotel hotel = requireHotel(id);
        hotel.reactivate();
        Hotel saved = hotelRepo.save(hotel);
        eventPublisher.publishEvent(new HotelReactivatedEvent(saved.getId()));
        return HotelResponse.from(saved);
    }

    private Hotel requireHotel(HotelId id) {
        return hotelRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel", id.value()));
    }
}
