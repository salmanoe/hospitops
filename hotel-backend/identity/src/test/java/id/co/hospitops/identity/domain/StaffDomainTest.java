package id.co.hospitops.identity.domain;

import id.co.hospitops.identity.domain.model.Staff;
import id.co.hospitops.identity.domain.model.StaffRole;
import id.co.hospitops.shared.HotelId;
import id.co.hospitops.shared.StaffId;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Staff Domain")
class StaffDomainTest {

    private Staff activeAdmin() {
        return Staff.create(HotelId.generate(), "John Admin", "jadmin",
                "$2a$12$hashedpassword", StaffRole.ADMIN);
    }

    private Staff reconstituted(boolean active) {
        return Staff.reconstitute(
                StaffId.generate(), "Jane Staff", "jstaff",
                "$2a$12$hash", StaffRole.FRONT_DESK,
                active, HotelId.generate(), LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("create()")
    class Create {
        @Test
        @DisplayName("assigns a generated ID")
        void assignsGeneratedId() {
            assertThat(activeAdmin().getId()).isNotNull();
            assertThat(activeAdmin().getId().value()).isNotNull();
        }

        @Test
        @DisplayName("is active by default")
        void isActiveByDefault() {
            assertThat(activeAdmin().isActive()).isTrue();
        }

        @Test
        @DisplayName("stores the provided role")
        void storesRole() {
            Staff staff = Staff.create(HotelId.generate(), "Name", "user", "$2a$12$hash", StaffRole.ACCOUNTANT);
            assertThat(staff.getRole()).isEqualTo(StaffRole.ACCOUNTANT);
        }

        @Test
        @DisplayName("sets createdAt to now")
        void setsCreatedAt() {
            LocalDateTime before = LocalDateTime.now().minusSeconds(1);
            assertThat(activeAdmin().getCreatedAt()).isAfter(before);
        }
    }

    @Nested
    @DisplayName("updateProfile()")
    class UpdateProfile {
        @Test
        @DisplayName("updates full name and bumps updatedAt")
        void updatesName() throws InterruptedException {
            Staff staff = activeAdmin();
            LocalDateTime before = staff.getUpdatedAt();
            Thread.sleep(1);
            staff.updateProfile("Updated Name");
            assertThat(staff.getFullName()).isEqualTo("Updated Name");
            assertThat(staff.getUpdatedAt()).isAfter(before);
        }

        @Test
        @DisplayName("rejects blank name")
        void rejectsBlankName() {
            assertThatThrownBy(() -> activeAdmin().updateProfile("   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Full name cannot be blank");
        }

        @Test
        @DisplayName("rejects null name")
        void rejectsNullName() {
            assertThatThrownBy(() -> activeAdmin().updateProfile(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("deactivate() / activate()")
    class ActivationToggle {
        @Test
        @DisplayName("deactivates an active staff member")
        void deactivatesActiveStaff() {
            Staff staff = activeAdmin();
            staff.deactivate();
            assertThat(staff.isActive()).isFalse();
        }

        @Test
        @DisplayName("reactivates a deactivated staff member")
        void reactivatesDeactivatedStaff() {
            Staff staff = reconstituted(false);
            staff.activate();
            assertThat(staff.isActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("hasRole() / canAccess()")
    class RoleChecks {
        @Test
        @DisplayName("hasRole() matches exact role")
        void hasRoleMatchesExact() {
            Staff manager = Staff.create(HotelId.generate(), "M", "m", "$h", StaffRole.MANAGER);
            assertThat(manager.hasRole(StaffRole.MANAGER)).isTrue();
            assertThat(manager.hasRole(StaffRole.ADMIN)).isFalse();
        }

        @Test
        @DisplayName("canAccess() matches any of multiple allowed roles")
        void canAccessMatchesAnyRole() {
            Staff frontDesk = Staff.create(HotelId.generate(), "F", "f", "$h", StaffRole.FRONT_DESK);
            assertThat(frontDesk.canAccess(StaffRole.ADMIN, StaffRole.MANAGER, StaffRole.FRONT_DESK)).isTrue();
            assertThat(frontDesk.canAccess(StaffRole.ADMIN, StaffRole.MANAGER)).isFalse();
        }
    }

    @Nested
    @DisplayName("changePassword()")
    class ChangePassword {
        @Test
        @DisplayName("updates password hash")
        void updatesHash() {
            Staff staff = activeAdmin();
            staff.changePassword("$2a$12$newhash");
            assertThat(staff.getPasswordHash()).isEqualTo("$2a$12$newhash");
        }

        @Test
        @DisplayName("rejects blank hash")
        void rejectsBlank() {
            assertThatThrownBy(() -> activeAdmin().changePassword(""))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
