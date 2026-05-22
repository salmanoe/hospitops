package id.co.hospitops.guest.domain;

import id.co.hospitops.guest.domain.model.Guest;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Guest Domain")
class GuestDomainTest {

    @Nested
    @DisplayName("create()")
    class Create {
        @Test
        void assignsId() {
            assertThat(guest().getId()).isNotNull();
        }

        @Test
        void storesFullName() {
            assertThat(guest().getFullName()).isEqualTo("John Doe");
        }

        @Test
        void rejectsBlankName() {
            assertThatThrownBy(() -> Guest.create("", null, null, null, null, null)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void rejectsNameTooLong() {
            String tooLong = "A".repeat(201);
            assertThatThrownBy(() -> Guest.create(tooLong, null, null, null, null, null)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("updateProfile()")
    class UpdateProfile {
        @Test
        void updatesAllFields() throws InterruptedException {
            Guest g = guest();
            Thread.sleep(1);
            g.updateProfile("Jane Doe", "US", "+1555", "jane@x.com", "NY");
            assertThat(g.getFullName()).isEqualTo("Jane Doe");
            assertThat(g.getNationality()).isEqualTo("US");
            assertThat(g.getUpdatedAt()).isAfter(g.getCreatedAt());
        }

        @Test
        void rejectsBlankName() {
            assertThatThrownBy(() -> guest().updateProfile("", null, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    private Guest guest() {
        return Guest.create("John Doe", "P123", "ID", "+62811", "j@e.com", "Jakarta");
    }
}
