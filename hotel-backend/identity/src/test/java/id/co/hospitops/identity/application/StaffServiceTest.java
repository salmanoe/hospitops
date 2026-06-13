package id.co.hospitops.identity.application;

import id.co.hospitops.identity.application.command.*;
import id.co.hospitops.identity.application.response.StaffResponse;
import id.co.hospitops.identity.domain.model.Staff;
import id.co.hospitops.identity.domain.model.StaffRole;
import id.co.hospitops.identity.domain.port.out.StaffRepository;
import id.co.hospitops.shared.HotelContext;
import id.co.hospitops.shared.HotelId;
import id.co.hospitops.shared.StaffId;
import id.co.hospitops.shared.exception.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StaffService")
class StaffServiceTest {

    @Mock
    StaffRepository staffRepository;
    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    StaffService staffService;

    @BeforeEach
    void setUp() {
        staffService = new StaffService(staffRepository, passwordEncoder);
    }

    @Nested
    @DisplayName("createStaff()")
    class CreateStaff {
        @Test
        @DisplayName("creates and returns staff when username is unique")
        void createsWhenUsernameUnique() {
            var command = new CreateStaffCommand("Budi Santoso", "budi", "secret123", StaffRole.FRONT_DESK);
            given(staffRepository.existsByUsername("budi")).willReturn(false);
            given(staffRepository.save(any(Staff.class))).willAnswer(inv -> inv.getArgument(0));

            // createStaff() reads HotelContext.current() to stamp the new staff with the
            // current hotel. In production the HotelContextInterceptor binds the ScopedValue;
            // in unit tests we bind it manually around the call under test.
            HotelId hotelId = HotelId.generate();
            StaffResponse[] holder = new StaffResponse[1];
            ScopedValue.where(HotelContext.HOTEL_ID, hotelId)
                    .run(() -> holder[0] = staffService.createStaff(command));
            StaffResponse response = holder[0];

            assertThat(response.fullName()).isEqualTo("Budi Santoso");
            assertThat(response.username()).isEqualTo("budi");
            assertThat(response.role()).isEqualTo(StaffRole.FRONT_DESK);
            assertThat(response.active()).isTrue();
        }

        @Test
        @DisplayName("throws ConflictException when username already exists")
        void throwsConflictWhenUsernameTaken() {
            var command = new CreateStaffCommand("Duplicate", "existing", "pass", StaffRole.FRONT_DESK);
            given(staffRepository.existsByUsername("existing")).willReturn(true);
            assertThatThrownBy(() -> staffService.createStaff(command))
                    .isInstanceOf(ConflictException.class).hasMessageContaining("existing");
            then(staffRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateStaff()")
    class UpdateStaff {
        @Test
        @DisplayName("updates name and role of existing staff")
        void updatesExistingStaff() {
            StaffId id = StaffId.generate();
            Staff existing = Staff.reconstitute(id, "Old Name", "user", "$2a$hash",
                    StaffRole.FRONT_DESK, true, HotelId.generate(), LocalDateTime.now(), LocalDateTime.now());
            given(staffRepository.findByIdInCurrentHotel(id)).willReturn(Optional.of(existing));
            given(staffRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            StaffResponse response = staffService.updateStaff(id, new UpdateStaffCommand("New Name", StaffRole.MANAGER));
            assertThat(response.fullName()).isEqualTo("New Name");
            assertThat(response.role()).isEqualTo(StaffRole.MANAGER);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when staff not found")
        void throwsNotFoundWhenMissing() {
            StaffId id = StaffId.generate();
            given(staffRepository.findByIdInCurrentHotel(id)).willReturn(Optional.empty());
            assertThatThrownBy(() -> staffService.updateStaff(id, new UpdateStaffCommand("Name", StaffRole.MANAGER)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("toggleActive()")
    class ToggleActive {
        @Test
        @DisplayName("deactivates an active staff member")
        void deactivatesActive() {
            StaffId id = StaffId.generate();
            Staff active = Staff.reconstitute(id, "Name", "user", "$h",
                    StaffRole.FRONT_DESK, true, HotelId.generate(), LocalDateTime.now(), LocalDateTime.now());
            given(staffRepository.findByIdInCurrentHotel(id)).willReturn(Optional.of(active));
            given(staffRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            staffService.toggleActive(id);
            then(staffRepository).should().save(argThat(s -> !s.isActive()));
        }

        @Test
        @DisplayName("activates an inactive staff member")
        void activatesInactive() {
            StaffId id = StaffId.generate();
            Staff inactive = Staff.reconstitute(id, "Name", "user", "$h",
                    StaffRole.FRONT_DESK, false, HotelId.generate(), LocalDateTime.now(), LocalDateTime.now());
            given(staffRepository.findByIdInCurrentHotel(id)).willReturn(Optional.of(inactive));
            given(staffRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            staffService.toggleActive(id);
            then(staffRepository).should().save(argThat(Staff::isActive));
        }
    }
}
