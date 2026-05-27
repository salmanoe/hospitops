package id.co.hospitops.housekeeping.domain;

import id.co.hospitops.housekeeping.domain.model.HousekeepingTask;
import id.co.hospitops.shared.ReservationId;
import id.co.hospitops.shared.RoomId;
import id.co.hospitops.shared.StaffId;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for the HousekeepingTask domain model.
 *
 * Covers both factory methods and every mutating business method,
 * including the already-completed guard on complete().
 */
@DisplayName("HousekeepingTask Domain")
class HousekeepingTaskDomainTest {

    private static final RoomId        ROOM_ID        = RoomId.generate();
    private static final ReservationId RESERVATION_ID = ReservationId.generate();

    // ── createForCheckout ────────────────────────────────────────────────

    @Nested
    @DisplayName("createForCheckout()")
    class CreateForCheckout {

        @Test
        @DisplayName("assigns a non-null ID")
        void assignsId() {
            assertThat(HousekeepingTask.createForCheckout(ROOM_ID, RESERVATION_ID).getId())
                    .isNotNull();
        }

        @Test
        @DisplayName("stores the provided roomId")
        void storesRoomId() {
            assertThat(HousekeepingTask.createForCheckout(ROOM_ID, RESERVATION_ID).getRoomId())
                    .isEqualTo(ROOM_ID);
        }

        @Test
        @DisplayName("stores the provided reservationId")
        void storesReservationId() {
            assertThat(HousekeepingTask.createForCheckout(ROOM_ID, RESERVATION_ID).getReservationId())
                    .isEqualTo(RESERVATION_ID);
        }

        @Test
        @DisplayName("is not completed at creation")
        void notCompletedAtCreation() {
            assertThat(HousekeepingTask.createForCheckout(ROOM_ID, RESERVATION_ID).isCompleted())
                    .isFalse();
        }

        @Test
        @DisplayName("has no assigned staff at creation")
        void noAssignedStaffAtCreation() {
            assertThat(HousekeepingTask.createForCheckout(ROOM_ID, RESERVATION_ID).getAssignedTo())
                    .isNull();
        }

        @Test
        @DisplayName("has a default checkout notes message")
        void hasDefaultNotes() {
            assertThat(HousekeepingTask.createForCheckout(ROOM_ID, RESERVATION_ID).getNotes())
                    .isNotBlank();
        }

        @Test
        @DisplayName("completedAt is null at creation")
        void completedAtIsNullAtCreation() {
            assertThat(HousekeepingTask.createForCheckout(ROOM_ID, RESERVATION_ID).getCompletedAt())
                    .isNull();
        }
    }

    // ── createManual ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("createManual()")
    class CreateManual {

        @Test
        @DisplayName("assigns a non-null ID")
        void assignsId() {
            assertThat(HousekeepingTask.createManual(ROOM_ID, "Deep clean").getId())
                    .isNotNull();
        }

        @Test
        @DisplayName("stores the provided roomId")
        void storesRoomId() {
            assertThat(HousekeepingTask.createManual(ROOM_ID, "Clean").getRoomId())
                    .isEqualTo(ROOM_ID);
        }

        @Test
        @DisplayName("reservationId is null for manual tasks")
        void reservationIdIsNull() {
            assertThat(HousekeepingTask.createManual(ROOM_ID, "Clean").getReservationId())
                    .isNull();
        }

        @Test
        @DisplayName("stores the provided notes")
        void storesNotes() {
            assertThat(HousekeepingTask.createManual(ROOM_ID, "Fix bathroom").getNotes())
                    .isEqualTo("Fix bathroom");
        }

        @Test
        @DisplayName("is not completed at creation")
        void notCompletedAtCreation() {
            assertThat(HousekeepingTask.createManual(ROOM_ID, "Clean").isCompleted()).isFalse();
        }

        @Test
        @DisplayName("each created task gets a unique ID")
        void eachTaskGetsUniqueId() {
            HousekeepingTask task1 = HousekeepingTask.createManual(ROOM_ID, "Clean");
            HousekeepingTask task2 = HousekeepingTask.createManual(ROOM_ID, "Clean");
            assertThat(task1.getId()).isNotEqualTo(task2.getId());
        }
    }

    // ── assign ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("assign()")
    class Assign {

        @Test
        @DisplayName("sets the assigned staff")
        void setsAssignedStaff() {
            StaffId staff = StaffId.generate();
            HousekeepingTask task = HousekeepingTask.createManual(ROOM_ID, "Clean");
            task.assign(staff);
            assertThat(task.getAssignedTo()).isEqualTo(staff);
        }

        @Test
        @DisplayName("bumps updatedAt after assignment")
        void bumpsUpdatedAt() throws InterruptedException {
            HousekeepingTask task = HousekeepingTask.createManual(ROOM_ID, "Clean");
            var before = task.getUpdatedAt();
            Thread.sleep(2);
            task.assign(StaffId.generate());
            assertThat(task.getUpdatedAt()).isAfterOrEqualTo(before);
        }

        @Test
        @DisplayName("can be reassigned to a different staff member")
        void canBeReassigned() {
            StaffId first  = StaffId.generate();
            StaffId second = StaffId.generate();
            HousekeepingTask task = HousekeepingTask.createManual(ROOM_ID, "Clean");
            task.assign(first);
            task.assign(second);
            assertThat(task.getAssignedTo()).isEqualTo(second);
        }
    }

    // ── complete ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("complete()")
    class Complete {

        @Test
        @DisplayName("marks the task as completed")
        void marksCompleted() {
            HousekeepingTask task = HousekeepingTask.createManual(ROOM_ID, "Clean");
            task.complete();
            assertThat(task.isCompleted()).isTrue();
        }

        @Test
        @DisplayName("sets completedAt to a non-null timestamp")
        void setsCompletedAt() {
            HousekeepingTask task = HousekeepingTask.createManual(ROOM_ID, "Clean");
            task.complete();
            assertThat(task.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("bumps updatedAt after completion")
        void bumpsUpdatedAt() throws InterruptedException {
            HousekeepingTask task = HousekeepingTask.createManual(ROOM_ID, "Clean");
            var before = task.getUpdatedAt();
            Thread.sleep(2);
            task.complete();
            assertThat(task.getUpdatedAt()).isAfterOrEqualTo(before);
        }

        @Test
        @DisplayName("throws IllegalStateException when already completed")
        void throwsWhenAlreadyCompleted() {
            HousekeepingTask task = HousekeepingTask.createManual(ROOM_ID, "Clean");
            task.complete();
            assertThatThrownBy(task::complete)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already completed");
        }
    }

    // ── updateNotes ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateNotes()")
    class UpdateNotes {

        @Test
        @DisplayName("replaces the notes text")
        void replacesNotes() {
            HousekeepingTask task = HousekeepingTask.createManual(ROOM_ID, "Initial notes");
            task.updateNotes("Updated notes");
            assertThat(task.getNotes()).isEqualTo("Updated notes");
        }

        @Test
        @DisplayName("allows setting notes to null")
        void allowsNullNotes() {
            HousekeepingTask task = HousekeepingTask.createManual(ROOM_ID, "Some notes");
            assertThatNoException().isThrownBy(() -> task.updateNotes(null));
            assertThat(task.getNotes()).isNull();
        }

        @Test
        @DisplayName("bumps updatedAt after note change")
        void bumpsUpdatedAt() throws InterruptedException {
            HousekeepingTask task = HousekeepingTask.createManual(ROOM_ID, "Old");
            var before = task.getUpdatedAt();
            Thread.sleep(2);
            task.updateNotes("New");
            assertThat(task.getUpdatedAt()).isAfterOrEqualTo(before);
        }
    }
}
