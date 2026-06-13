package id.co.hospitops.hotel.application;

import id.co.hospitops.hotel.application.command.SavePolicyConfigCommand;
import id.co.hospitops.hotel.application.response.PolicyConfigResponse;
import id.co.hospitops.hotel.domain.model.Hotel;
import id.co.hospitops.hotel.domain.model.HotelStatus;
import id.co.hospitops.hotel.domain.model.PolicyConfig;
import id.co.hospitops.hotel.domain.model.SetupStep;
import id.co.hospitops.hotel.domain.port.out.HotelPolicyConfigRepository;
import id.co.hospitops.hotel.domain.port.out.HotelRepository;
import id.co.hospitops.shared.GroupId;
import id.co.hospitops.shared.HotelId;
import id.co.hospitops.shared.PolicyConfigId;
import id.co.hospitops.shared.event.HotelActivatedEvent;
import id.co.hospitops.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("HotelPolicyService")
class HotelPolicyServiceTest {

    @Mock HotelPolicyConfigRepository policyRepo;
    @Mock HotelRepository hotelRepo;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks HotelPolicyService service;

    private static final HotelId HOTEL_ID = HotelId.generate();
    private static final GroupId GROUP_ID = GroupId.generate();

    private SavePolicyConfigCommand validCommand() {
        return new SavePolicyConfigCommand(
                HOTEL_ID, 11, "PPN", "Grand Palace Hotel",
                "Jl. Sudirman 1", "Thank you for your stay.");
    }

    private PolicyConfig savedConfig() {
        return PolicyConfig.reconstitute(
                PolicyConfigId.generate(), HOTEL_ID,
                11, "PPN", "Grand Palace Hotel", "Jl. Sudirman 1", "Thank you.",
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Nested
    @DisplayName("savePolicyConfig()")
    class SavePolicyConfig {

        @Test
        @DisplayName("creates a new config when none exists")
        void createNew() {
            given(policyRepo.findByHotelId(HOTEL_ID)).willReturn(Optional.empty());
            given(policyRepo.save(any())).willReturn(savedConfig());
            given(hotelRepo.findById(HOTEL_ID)).willReturn(Optional.empty());

            PolicyConfigResponse result = service.savePolicyConfig(validCommand());

            assertThat(result.hotelId()).isEqualTo(HOTEL_ID.value());
            then(policyRepo).should().save(any(PolicyConfig.class));
        }

        @Test
        @DisplayName("updates existing config in place (upsert)")
        void updateExisting() {
            PolicyConfig existing = savedConfig();
            given(policyRepo.findByHotelId(HOTEL_ID)).willReturn(Optional.of(existing));
            given(policyRepo.save(existing)).willReturn(existing);
            given(hotelRepo.findById(HOTEL_ID)).willReturn(Optional.empty());

            service.savePolicyConfig(validCommand());

            // The existing instance is mutated and re-saved, not replaced
            then(policyRepo).should().save(existing);
        }

        @Test
        @DisplayName("marks POLICY step and does NOT publish activation when other steps remain")
        void marksPolicyStepWithoutActivation() {
            given(policyRepo.findByHotelId(HOTEL_ID)).willReturn(Optional.empty());
            given(policyRepo.save(any())).willReturn(savedConfig());

            // Hotel in SETUP, POLICY not yet done, other steps also incomplete
            Hotel hotelInSetup = Hotel.create(GROUP_ID, "Grand Palace");
            given(hotelRepo.findById(HOTEL_ID)).willReturn(Optional.of(hotelInSetup));
            given(hotelRepo.save(any())).willReturn(hotelInSetup);

            service.savePolicyConfig(validCommand());

            ArgumentCaptor<Hotel> saved = ArgumentCaptor.forClass(Hotel.class);
            then(hotelRepo).should().save(saved.capture());
            assertThat(saved.getValue().getChecklist().isPolicyComplete()).isTrue();
            assertThat(saved.getValue().getStatus()).isEqualTo(HotelStatus.SETUP);
            then(eventPublisher).should(never()).publishEvent(any(HotelActivatedEvent.class));
        }

        @Test
        @DisplayName("auto-activates hotel and publishes event when POLICY was the last step")
        void autoActivatesWhenLastStep() {
            given(policyRepo.findByHotelId(HOTEL_ID)).willReturn(Optional.empty());
            given(policyRepo.save(any())).willReturn(savedConfig());

            // Build a hotel with all steps complete except POLICY
            Hotel hotel = Hotel.create(GROUP_ID, "Grand Palace");
            hotel.completeSetupStep(SetupStep.PROFILE);
            hotel.completeSetupStep(SetupStep.ROOM_TYPE);
            hotel.completeSetupStep(SetupStep.ROOM);
            hotel.completeSetupStep(SetupStep.STAFF_ACCOUNT);
            given(hotelRepo.findById(HOTEL_ID)).willReturn(Optional.of(hotel));
            given(hotelRepo.save(any())).willReturn(hotel);

            service.savePolicyConfig(validCommand());

            ArgumentCaptor<Hotel> saved = ArgumentCaptor.forClass(Hotel.class);
            then(hotelRepo).should().save(saved.capture());
            assertThat(saved.getValue().getStatus()).isEqualTo(HotelStatus.ACTIVE);
            then(eventPublisher).should().publishEvent(any(HotelActivatedEvent.class));
        }

        @Test
        @DisplayName("does not touch the hotel when it is already ACTIVE")
        void skipsAlreadyActiveHotel() {
            given(policyRepo.findByHotelId(HOTEL_ID)).willReturn(Optional.empty());
            given(policyRepo.save(any())).willReturn(savedConfig());

            // Build an ACTIVE hotel (all steps complete)
            Hotel hotel = Hotel.create(GROUP_ID, "Grand Palace");
            hotel.completeSetupStep(SetupStep.PROFILE);
            hotel.completeSetupStep(SetupStep.POLICY);
            hotel.completeSetupStep(SetupStep.ROOM_TYPE);
            hotel.completeSetupStep(SetupStep.ROOM);
            hotel.completeSetupStep(SetupStep.STAFF_ACCOUNT);
            given(hotelRepo.findById(HOTEL_ID)).willReturn(Optional.of(hotel));

            service.savePolicyConfig(validCommand());

            then(hotelRepo).should(never()).save(any());
            then(eventPublisher).should(never()).publishEvent(any());
        }
    }

    @Nested
    @DisplayName("findByHotelId()")
    class FindByHotelId {

        @Test
        @DisplayName("returns config when present")
        void found() {
            given(policyRepo.findByHotelId(HOTEL_ID)).willReturn(Optional.of(savedConfig()));

            PolicyConfigResponse result = service.findByHotelId(HOTEL_ID);

            assertThat(result.taxPercent()).isEqualTo(11);
            assertThat(result.taxName()).isEqualTo("PPN");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when no config exists")
        void notFound() {
            given(policyRepo.findByHotelId(HOTEL_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.findByHotelId(HOTEL_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
