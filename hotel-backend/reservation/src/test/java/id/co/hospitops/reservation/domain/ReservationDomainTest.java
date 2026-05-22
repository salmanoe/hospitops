package id.co.hospitops.reservation.domain;

import id.co.hospitops.reservation.domain.model.*;
import id.co.hospitops.shared.*;
import org.junit.jupiter.api.*;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Reservation Domain")
class ReservationDomainTest {

    private Reservation res() {
        return Reservation.create("RES-2025-00001",
                GuestId.generate(), RoomId.generate(),
                LocalDate.now(), LocalDate.now().plusDays(3),
                Money.of(800_000L), 2, 0, null, StaffId.generate());
    }

    @Test
    void rejectsCheckOutBeforeCheckIn() {
        assertThatThrownBy(() -> Reservation.create("RES-X",
                GuestId.generate(), RoomId.generate(),
                LocalDate.now(), LocalDate.now(),
                Money.of(100L), 1, 0, null, StaffId.generate()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void calculatesNightsCorrectly() {
        assertThat(res().getNights()).isEqualTo(3);
    }

    @Test
    void calculatesSubtotalCorrectly() {
        assertThat(res().calculateSubtotal().amount())
                .isEqualByComparingTo(java.math.BigDecimal.valueOf(2_400_000));
    }

    @Nested
    @DisplayName("checkIn()")
    class CheckIn {
        @Test
        void succeedsWhenConfirmed() {
            Reservation r = res();
            r.checkIn();
            assertThat(r.getStatus()).isEqualTo(ReservationStatus.CHECKED_IN);
        }

        @Test
        void failsWhenAlreadyCheckedIn() {
            Reservation r = res();
            r.checkIn();
            assertThatThrownBy(r::checkIn).isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("checkOut()")
    class CheckOut {
        @Test
        void succeedsWhenCheckedIn() {
            Reservation r = res();
            r.checkIn();
            r.checkOut();
            assertThat(r.getStatus()).isEqualTo(ReservationStatus.CHECKED_OUT);
        }

        @Test
        void failsWhenNotCheckedIn() {
            assertThatThrownBy(res()::checkOut).isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("cancel()")
    class Cancel {
        @Test
        void succeedsWhenConfirmed() {
            Reservation r = res();
            r.cancel();
            assertThat(r.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        }

        @Test
        void failsWhenCheckedIn() {
            Reservation r = res();
            r.checkIn();
            assertThatThrownBy(r::cancel).isInstanceOf(IllegalStateException.class);
        }

        @Test
        void failsWhenCheckedOut() {
            Reservation r = res();
            r.checkIn();
            r.checkOut();
            assertThatThrownBy(r::cancel).isInstanceOf(IllegalStateException.class);
        }
    }
}
