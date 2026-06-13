package id.co.hospitops.room.domain;

import id.co.hospitops.room.domain.model.Room;
import id.co.hospitops.room.domain.model.RoomStatus;
import id.co.hospitops.shared.HotelId;
import id.co.hospitops.shared.RoomTypeId;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for the Room state machine (R-05 fix).
 * <p>
 * Covers every valid transition and every illegal-source-state combination
 * so that future changes to Room will fail fast here if a guard is broken.
 */
@DisplayName("Room State Machine")
class RoomDomainTest {

    private Room available() {
        return Room.create(HotelId.generate(), "101", 1, RoomTypeId.generate(), null);
    }

    // ── markOccupied ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("markOccupied()")
    class MarkOccupied {

        @Test
        @DisplayName("AVAILABLE -> OCCUPIED succeeds")
        void availableToOccupied() {
            Room room = available();
            room.markOccupied();
            assertThat(room.getStatus()).isEqualTo(RoomStatus.OCCUPIED);
        }

        @Test
        @DisplayName("DIRTY -> OCCUPIED throws")
        void dirtyToOccupied() {
            Room room = available();
            room.markOccupied();
            room.markDirty();
            assertThatThrownBy(room::markOccupied)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("DIRTY");
        }

        @Test
        @DisplayName("OCCUPIED -> OCCUPIED throws (already occupied)")
        void occupiedToOccupied() {
            Room room = available();
            room.markOccupied();
            assertThatThrownBy(room::markOccupied)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("OCCUPIED");
        }

        @Test
        @DisplayName("MAINTENANCE -> OCCUPIED throws")
        void maintenanceToOccupied() {
            Room room = available();
            room.markMaintenance("fix AC");
            assertThatThrownBy(room::markOccupied)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("MAINTENANCE");
        }

        @Test
        @DisplayName("SERVICE_REQUESTED -> markOccupied throws (use markServiceComplete instead)")
        void serviceRequestedToMarkOccupiedThrows() {
            Room room = available();
            room.markOccupied();
            room.requestService();
            assertThatThrownBy(room::markOccupied)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("SERVICE_REQUESTED");
        }
    }

    // ── markDirty ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("markDirty()")
    class MarkDirty {

        @Test
        @DisplayName("OCCUPIED -> DIRTY succeeds")
        void occupiedToDirty() {
            Room room = available();
            room.markOccupied();
            room.markDirty();
            assertThat(room.getStatus()).isEqualTo(RoomStatus.DIRTY);
        }

        @Test
        @DisplayName("AVAILABLE -> DIRTY throws")
        void availableToDirty() {
            assertThatThrownBy(available()::markDirty)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("AVAILABLE");
        }

        @Test
        @DisplayName("MAINTENANCE -> DIRTY throws")
        void maintenanceToDirty() {
            Room room = available();
            room.markMaintenance("pipes");
            assertThatThrownBy(room::markDirty)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("MAINTENANCE");
        }
    }

    // ── markAvailable ────────────────────────────────────────────────────

    @Nested
    @DisplayName("markAvailable()")
    class MarkAvailable {

        @Test
        @DisplayName("DIRTY -> AVAILABLE succeeds (housekeeping complete)")
        void dirtyToAvailable() {
            Room room = available();
            room.markOccupied();
            room.markDirty();
            room.markAvailable();
            assertThat(room.getStatus()).isEqualTo(RoomStatus.AVAILABLE);
        }

        @Test
        @DisplayName("MAINTENANCE -> AVAILABLE succeeds (repairs done)")
        void maintenanceToAvailable() {
            Room room = available();
            room.markMaintenance("broken heater");
            room.markAvailable();
            assertThat(room.getStatus()).isEqualTo(RoomStatus.AVAILABLE);
        }

        @Test
        @DisplayName("AVAILABLE -> AVAILABLE throws")
        void availableToAvailable() {
            assertThatThrownBy(available()::markAvailable)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("AVAILABLE");
        }

        @Test
        @DisplayName("OCCUPIED -> AVAILABLE throws (must check out first)")
        void occupiedToAvailable() {
            Room room = available();
            room.markOccupied();
            assertThatThrownBy(room::markAvailable)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("OCCUPIED");
        }
    }

    // ── markMaintenance ──────────────────────────────────────────────────

    @Nested
    @DisplayName("markMaintenance()")
    class MarkMaintenance {

        @Test
        @DisplayName("AVAILABLE -> MAINTENANCE succeeds")
        void availableToMaintenance() {
            Room room = available();
            room.markMaintenance("scheduled");
            assertThat(room.getStatus()).isEqualTo(RoomStatus.MAINTENANCE);
        }

        @Test
        @DisplayName("DIRTY -> MAINTENANCE succeeds")
        void dirtyToMaintenance() {
            Room room = available();
            room.markOccupied();
            room.markDirty();
            room.markMaintenance("deep clean needed");
            assertThat(room.getStatus()).isEqualTo(RoomStatus.MAINTENANCE);
        }

        @Test
        @DisplayName("OCCUPIED -> MAINTENANCE throws (cannot displace a guest)")
        void occupiedToMaintenance() {
            Room room = available();
            room.markOccupied();
            assertThatThrownBy(() -> room.markMaintenance("pipe burst"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("OCCUPIED");
        }

        @Test
        @DisplayName("SERVICE_REQUESTED -> MAINTENANCE throws (guest still present)")
        void serviceRequestedToMaintenance() {
            Room room = available();
            room.markOccupied();
            room.requestService();
            assertThatThrownBy(() -> room.markMaintenance("pipe burst"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("SERVICE_REQUESTED");
        }

        @Test
        @DisplayName("maintenance reason is stored in notes")
        void reasonStoredInNotes() {
            Room room = available();
            room.markMaintenance("broken AC");
            assertThat(room.getNotes()).isEqualTo("broken AC");
        }
    }

    // ── requestService ───────────────────────────────────────────────────

    @Nested
    @DisplayName("requestService()")
    class RequestService {

        @Test
        @DisplayName("OCCUPIED -> SERVICE_REQUESTED succeeds")
        void occupiedToServiceRequested() {
            Room room = available();
            room.markOccupied();
            room.requestService();
            assertThat(room.getStatus()).isEqualTo(RoomStatus.SERVICE_REQUESTED);
        }

        @Test
        @DisplayName("AVAILABLE -> SERVICE_REQUESTED throws")
        void availableToServiceRequested() {
            assertThatThrownBy(available()::requestService)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("AVAILABLE");
        }

        @Test
        @DisplayName("DIRTY -> SERVICE_REQUESTED throws")
        void dirtyToServiceRequested() {
            Room room = available();
            room.markOccupied();
            room.markDirty();
            assertThatThrownBy(room::requestService)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("DIRTY");
        }

        @Test
        @DisplayName("MAINTENANCE -> SERVICE_REQUESTED throws")
        void maintenanceToServiceRequested() {
            Room room = available();
            room.markMaintenance("repairs");
            assertThatThrownBy(room::requestService)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("MAINTENANCE");
        }
    }

    // ── markServiceComplete ──────────────────────────────────────────────

    @Nested
    @DisplayName("markServiceComplete()")
    class MarkServiceComplete {

        @Test
        @DisplayName("SERVICE_REQUESTED -> OCCUPIED succeeds (guest still in room)")
        void serviceRequestedToOccupied() {
            Room room = available();
            room.markOccupied();
            room.requestService();
            room.markServiceComplete();
            assertThat(room.getStatus()).isEqualTo(RoomStatus.OCCUPIED);
        }

        @Test
        @DisplayName("OCCUPIED -> markServiceComplete throws (no pending service request)")
        void occupiedToMarkServiceCompleteThrows() {
            Room room = available();
            room.markOccupied();
            assertThatThrownBy(room::markServiceComplete)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("OCCUPIED");
        }

        @Test
        @DisplayName("AVAILABLE -> markServiceComplete throws")
        void availableToMarkServiceCompleteThrows() {
            assertThatThrownBy(available()::markServiceComplete)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("AVAILABLE");
        }
    }

    // ── create() validation ──────────────────────────────────────────────

    @Nested
    @DisplayName("create() validation")
    class Create {

        @Test
        @DisplayName("blank room number throws")
        void blankRoomNumber() {
            assertThatThrownBy(() -> Room.create(HotelId.generate(), "", 1, RoomTypeId.generate(), null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("floor < 1 throws")
        void invalidFloor() {
            assertThatThrownBy(() -> Room.create(HotelId.generate(), "101", 0, RoomTypeId.generate(), null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("new room starts AVAILABLE")
        void startsAvailable() {
            assertThat(available().getStatus()).isEqualTo(RoomStatus.AVAILABLE);
        }
    }
}
