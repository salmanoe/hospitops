package id.co.hospitops.hotel.application;

import id.co.hospitops.hotel.application.command.CompleteSetupStepCommand;
import id.co.hospitops.hotel.application.command.CreateHotelCommand;
import id.co.hospitops.hotel.application.response.HotelResponse;
import id.co.hospitops.hotel.domain.model.*;
import id.co.hospitops.hotel.domain.port.out.HotelRepository;
import id.co.hospitops.shared.GroupId;
import id.co.hospitops.shared.HotelId;
import id.co.hospitops.shared.event.HotelActivatedEvent;
import id.co.hospitops.shared.event.HotelReactivatedEvent;
import id.co.hospitops.shared.event.HotelSuspendedEvent;
import id.co.hospitops.shared.exception.BusinessRuleViolationException;
import id.co.hospitops.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("HotelService")
@ExtendWith(MockitoExtension.class)
class HotelServiceTest {

    @Mock HotelRepository hotelRepo;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks HotelService service;

    @Nested
    @DisplayName("createHotel()")
    class CreateHotel {

        @Test
        @DisplayName("saves and returns a SETUP hotel")
        void createsSetupHotel() {
            GroupId groupId = GroupId.generate();
            var cmd = new CreateHotelCommand(groupId, "Grand Palace");
            Hotel saved = Hotel.create(groupId, "Grand Palace");
            when(hotelRepo.save(any())).thenReturn(saved);

            HotelResponse response = service.createHotel(cmd);

            assertThat(response.status()).isEqualTo(HotelStatus.SETUP);
            assertThat(response.name()).isEqualTo("Grand Palace");
            verify(hotelRepo).save(any(Hotel.class));
        }
    }

    @Nested
    @DisplayName("completeSetupStep()")
    class CompleteSetupStep {

        @Test
        @DisplayName("does not publish event when hotel stays in SETUP")
        void noEventWhenNotActivated() {
            GroupId groupId = GroupId.generate();
            Hotel hotel = Hotel.create(groupId, "Grand Palace");
            when(hotelRepo.findById(hotel.getId())).thenReturn(Optional.of(hotel));
            when(hotelRepo.save(any())).thenReturn(hotel);

            service.completeSetupStep(new CompleteSetupStepCommand(groupId, hotel.getId(), SetupStep.PROFILE));

            verify(eventPublisher, never()).publishEvent(any(HotelActivatedEvent.class));
        }

        @Test
        @DisplayName("publishes HotelActivatedEvent when hotel transitions to ACTIVE")
        void publishesEventOnActivation() {
            GroupId groupId = GroupId.generate();
            Hotel hotel = partiallySetupHotel(groupId,
                    SetupStep.PROFILE, SetupStep.POLICY, SetupStep.ROOM_TYPE, SetupStep.ROOM);
            when(hotelRepo.findById(hotel.getId())).thenReturn(Optional.of(hotel));
            when(hotelRepo.save(any())).thenReturn(hotel);

            service.completeSetupStep(
                    new CompleteSetupStepCommand(groupId, hotel.getId(), SetupStep.STAFF_ACCOUNT));

            verify(eventPublisher).publishEvent(any(HotelActivatedEvent.class));
        }

        @Test
        @DisplayName("throws ResourceNotFoundException for unknown hotel")
        void throwsForUnknownHotel() {
            HotelId unknown = HotelId.generate();
            when(hotelRepo.findById(unknown)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    service.completeSetupStep(new CompleteSetupStepCommand(GroupId.generate(), unknown, SetupStep.PROFILE)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("throws BusinessRuleViolationException when hotel belongs to a different group")
        void throwsForWrongGroup() {
            Hotel hotel = Hotel.create(GroupId.generate(), "Grand Palace");
            when(hotelRepo.findById(hotel.getId())).thenReturn(Optional.of(hotel));

            GroupId otherGroup = GroupId.generate();
            assertThatThrownBy(() ->
                    service.completeSetupStep(new CompleteSetupStepCommand(otherGroup, hotel.getId(), SetupStep.PROFILE)))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("does not belong to your group");
        }
    }

    @Nested
    @DisplayName("suspend()")
    class Suspend {

        @Test
        @DisplayName("publishes HotelSuspendedEvent")
        void publishesSuspendedEvent() {
            GroupId groupId = GroupId.generate();
            Hotel hotel = activeHotel(groupId);
            when(hotelRepo.findById(hotel.getId())).thenReturn(Optional.of(hotel));
            when(hotelRepo.save(any())).thenReturn(hotel);

            service.suspend(hotel.getId(), groupId);

            verify(eventPublisher).publishEvent(any(HotelSuspendedEvent.class));
        }

        @Test
        @DisplayName("throws BusinessRuleViolationException when hotel is not ACTIVE")
        void throwsWhenNotActive() {
            GroupId groupId = GroupId.generate();
            Hotel hotel = Hotel.create(groupId, "Grand Palace");
            when(hotelRepo.findById(hotel.getId())).thenReturn(Optional.of(hotel));

            assertThatThrownBy(() -> service.suspend(hotel.getId(), groupId))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }

        @Test
        @DisplayName("throws BusinessRuleViolationException when hotel belongs to a different group")
        void throwsForWrongGroup() {
            Hotel hotel = activeHotel(GroupId.generate());
            when(hotelRepo.findById(hotel.getId())).thenReturn(Optional.of(hotel));

            assertThatThrownBy(() -> service.suspend(hotel.getId(), GroupId.generate()))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("does not belong to your group");
        }
    }

    @Nested
    @DisplayName("reactivate()")
    class Reactivate {

        @Test
        @DisplayName("transitions SUSPENDED → ACTIVE and publishes HotelReactivatedEvent")
        void publishesReactivatedEvent() {
            GroupId groupId = GroupId.generate();
            Hotel hotel = activeHotel(groupId);
            hotel.suspend();
            when(hotelRepo.findById(hotel.getId())).thenReturn(Optional.of(hotel));
            when(hotelRepo.save(any())).thenReturn(hotel);

            HotelResponse response = service.reactivate(hotel.getId(), groupId);

            assertThat(response.status()).isEqualTo(HotelStatus.ACTIVE);
            verify(eventPublisher).publishEvent(any(HotelReactivatedEvent.class));
            // HotelActivatedEvent must NOT fire — that is reserved for SETUP → ACTIVE
            verify(eventPublisher, never()).publishEvent(any(HotelActivatedEvent.class));
        }

        @Test
        @DisplayName("throws BusinessRuleViolationException when hotel is not SUSPENDED")
        void throwsWhenNotSuspended() {
            GroupId groupId = GroupId.generate();
            Hotel hotel = activeHotel(groupId);
            when(hotelRepo.findById(hotel.getId())).thenReturn(Optional.of(hotel));

            assertThatThrownBy(() -> service.reactivate(hotel.getId(), groupId))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }

        @Test
        @DisplayName("throws BusinessRuleViolationException when hotel belongs to a different group")
        void throwsForWrongGroup() {
            Hotel hotel = activeHotel(GroupId.generate());
            hotel.suspend();
            when(hotelRepo.findById(hotel.getId())).thenReturn(Optional.of(hotel));

            assertThatThrownBy(() -> service.reactivate(hotel.getId(), GroupId.generate()))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("does not belong to your group");
        }
    }

    @Nested
    @DisplayName("findByGroupId()")
    class FindByGroupId {

        @Test
        @DisplayName("returns all hotels for a group")
        void returnsHotels() {
            GroupId groupId = GroupId.generate();
            Hotel h1 = Hotel.create(groupId, "Hotel A");
            Hotel h2 = Hotel.create(groupId, "Hotel B");
            when(hotelRepo.findByGroupId(groupId)).thenReturn(List.of(h1, h2));

            List<HotelResponse> results = service.findByGroupId(groupId);

            assertThat(results).hasSize(2);
        }
    }

    // ── helpers ───────────────────────────────────────────────────

    private Hotel partiallySetupHotel(GroupId groupId, SetupStep... steps) {
        Hotel h = Hotel.create(groupId, "Grand Palace");
        for (SetupStep step : steps) h.completeSetupStep(step);
        return h;
    }

    private Hotel activeHotel(GroupId groupId) {
        return partiallySetupHotel(groupId, SetupStep.values());
    }
}
