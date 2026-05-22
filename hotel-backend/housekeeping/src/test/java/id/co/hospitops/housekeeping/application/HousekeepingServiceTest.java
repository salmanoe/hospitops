package id.co.hospitops.housekeeping.application;

import id.co.hospitops.housekeeping.application.response.HousekeepingTaskResponse;
import id.co.hospitops.housekeeping.application.response.RoomStatusResponse;
import id.co.hospitops.housekeeping.domain.model.HousekeepingTask;
import id.co.hospitops.housekeeping.domain.port.out.HousekeepingTaskRepository;
import id.co.hospitops.housekeeping.domain.port.out.RoomStatusPort;
import id.co.hospitops.housekeeping.domain.port.out.RoomStatusPort.RoomBoardEntry;
import id.co.hospitops.shared.ReservationId;
import id.co.hospitops.shared.RoomId;
import id.co.hospitops.shared.StaffId;
import id.co.hospitops.shared.exception.ResourceNotFoundException;
import id.co.hospitops.shared.web.PageResult;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for HousekeepingService — first test coverage for the housekeeping module.
 *
 * Covers:
 *   - assignTask()         : not-found, assigns staff + saves
 *   - completeTask()       : not-found, already-completed guard, happy path
 *   - createManualTask()   : delegates domain factory, saves, returns response
 *   - createCheckoutTask() : delegates domain factory, saves
 *   - findPendingTasks()   : pagination delegation + countPending() total
 *   - getBoardByFloor()    : floor grouping via RoomStatusPort, ascending order
 */
@DisplayName("HousekeepingService")
@ExtendWith(MockitoExtension.class)
class HousekeepingServiceTest {

    @Mock HousekeepingTaskRepository taskRepo;
    @Mock RoomStatusPort             roomStatusPort;

    @InjectMocks HousekeepingService service;

    // ── helpers ────────────────────────────────────────────────────

    private static HousekeepingTask stubPendingTask() {
        return HousekeepingTask.createManual(RoomId.generate(), "Routine cleaning");
    }

    private static HousekeepingTask stubCompletedTask() {
        HousekeepingTask t = stubPendingTask();
        t.complete();
        return t;
    }

    // ══════════════════════════════════════════════════════════════
    // assignTask()
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("assignTask()")
    class AssignTask {

        @Test
        @DisplayName("throws ResourceNotFoundException when task does not exist")
        void throwsWhenNotFound() {
            UUID taskId = UUID.randomUUID();
            when(taskRepo.findById(taskId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.assignTask(taskId, StaffId.generate()))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(taskRepo, never()).save(any());
        }

        @Test
        @DisplayName("assigns staff, saves, and returns response")
        void assignsStaffAndSaves() {
            UUID             taskId  = UUID.randomUUID();
            StaffId          staff   = StaffId.generate();
            HousekeepingTask pending = stubPendingTask();
            when(taskRepo.findById(taskId)).thenReturn(Optional.of(pending));
            when(taskRepo.save(pending)).thenReturn(pending);

            HousekeepingTaskResponse result = service.assignTask(taskId, staff);

            assertThat(result).isNotNull();
            assertThat(pending.getAssignedTo()).isEqualTo(staff);
            verify(taskRepo).save(pending);
        }

        @Test
        @DisplayName("saves exactly once per assign call")
        void savesExactlyOnce() {
            UUID             taskId  = UUID.randomUUID();
            HousekeepingTask pending = stubPendingTask();
            when(taskRepo.findById(taskId)).thenReturn(Optional.of(pending));
            when(taskRepo.save(pending)).thenReturn(pending);

            service.assignTask(taskId, StaffId.generate());

            verify(taskRepo, times(1)).save(pending);
        }
    }

    // ══════════════════════════════════════════════════════════════
    // completeTask()
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("completeTask()")
    class CompleteTask {

        @Test
        @DisplayName("throws ResourceNotFoundException when task does not exist")
        void throwsWhenNotFound() {
            UUID taskId = UUID.randomUUID();
            when(taskRepo.findById(taskId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.completeTask(taskId))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(taskRepo, never()).save(any());
        }

        @Test
        @DisplayName("throws IllegalStateException when task is already completed")
        void throwsWhenAlreadyCompleted() {
            UUID             taskId    = UUID.randomUUID();
            HousekeepingTask completed = stubCompletedTask();
            when(taskRepo.findById(taskId)).thenReturn(Optional.of(completed));

            assertThatThrownBy(() -> service.completeTask(taskId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already completed");

            verify(taskRepo, never()).save(any());
        }

        @Test
        @DisplayName("marks task completed, saves, and returns response")
        void completesAndSaves() {
            UUID             taskId  = UUID.randomUUID();
            HousekeepingTask pending = stubPendingTask();
            when(taskRepo.findById(taskId)).thenReturn(Optional.of(pending));
            when(taskRepo.save(pending)).thenReturn(pending);

            HousekeepingTaskResponse result = service.completeTask(taskId);

            assertThat(result).isNotNull();
            assertThat(pending.isCompleted()).isTrue();
            assertThat(pending.getCompletedAt()).isNotNull();
            verify(taskRepo).save(pending);
        }

        @Test
        @DisplayName("saves exactly once per complete call")
        void savesExactlyOnce() {
            UUID             taskId  = UUID.randomUUID();
            HousekeepingTask pending = stubPendingTask();
            when(taskRepo.findById(taskId)).thenReturn(Optional.of(pending));
            when(taskRepo.save(pending)).thenReturn(pending);

            service.completeTask(taskId);

            verify(taskRepo, times(1)).save(pending);
            verifyNoMoreInteractions(taskRepo);
        }
    }

    // ══════════════════════════════════════════════════════════════
    // createManualTask()
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("createManualTask()")
    class CreateManualTask {

        @Test
        @DisplayName("saves task and returns response")
        void savesAndReturnsResponse() {
            RoomId           roomId = RoomId.generate();
            HousekeepingTask task   = HousekeepingTask.createManual(roomId, "Deep clean required");
            when(taskRepo.save(any(HousekeepingTask.class))).thenReturn(task);

            HousekeepingTaskResponse result = service.createManualTask(roomId, "Deep clean required");

            assertThat(result).isNotNull();
            verify(taskRepo).save(any(HousekeepingTask.class));
        }

        @Test
        @DisplayName("task has no reservationId (manual trigger)")
        void taskHasNullReservationId() {
            RoomId           roomId = RoomId.generate();
            HousekeepingTask task   = HousekeepingTask.createManual(roomId, "Spot clean");
            when(taskRepo.save(any(HousekeepingTask.class))).thenReturn(task);

            service.createManualTask(roomId, "Spot clean");

            ArgumentCaptor<HousekeepingTask> captor = ArgumentCaptor.forClass(HousekeepingTask.class);
            verify(taskRepo).save(captor.capture());
            assertThat(captor.getValue().getReservationId()).isNull();
        }

        @Test
        @DisplayName("task is not completed at creation time")
        void taskIsNotCompletedAtCreation() {
            RoomId           roomId = RoomId.generate();
            HousekeepingTask task   = HousekeepingTask.createManual(roomId, "Polish floors");
            when(taskRepo.save(any(HousekeepingTask.class))).thenReturn(task);

            service.createManualTask(roomId, "Polish floors");

            ArgumentCaptor<HousekeepingTask> captor = ArgumentCaptor.forClass(HousekeepingTask.class);
            verify(taskRepo).save(captor.capture());
            assertThat(captor.getValue().isCompleted()).isFalse();
        }
    }

    // ══════════════════════════════════════════════════════════════
    // createCheckoutTask()
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("createCheckoutTask()")
    class CreateCheckoutTask {

        @Test
        @DisplayName("saves task and returns domain object")
        void savesAndReturnsTask() {
            RoomId          roomId        = RoomId.generate();
            ReservationId   reservationId = ReservationId.generate();
            HousekeepingTask task = HousekeepingTask.createForCheckout(roomId, reservationId);
            when(taskRepo.save(any(HousekeepingTask.class))).thenReturn(task);

            HousekeepingTask result = service.createCheckoutTask(roomId, reservationId);

            assertThat(result).isNotNull();
            verify(taskRepo).save(any(HousekeepingTask.class));
        }

        @Test
        @DisplayName("task carries the reservationId as trigger context")
        void taskCarriesReservationId() {
            RoomId          roomId        = RoomId.generate();
            ReservationId   reservationId = ReservationId.generate();
            HousekeepingTask task = HousekeepingTask.createForCheckout(roomId, reservationId);
            when(taskRepo.save(any(HousekeepingTask.class))).thenReturn(task);

            service.createCheckoutTask(roomId, reservationId);

            ArgumentCaptor<HousekeepingTask> captor = ArgumentCaptor.forClass(HousekeepingTask.class);
            verify(taskRepo).save(captor.capture());
            assertThat(captor.getValue().getReservationId()).isEqualTo(reservationId);
        }

        @Test
        @DisplayName("task is not completed at creation time")
        void taskIsNotCompletedAtCreation() {
            RoomId          roomId        = RoomId.generate();
            ReservationId   reservationId = ReservationId.generate();
            HousekeepingTask task = HousekeepingTask.createForCheckout(roomId, reservationId);
            when(taskRepo.save(any(HousekeepingTask.class))).thenReturn(task);

            service.createCheckoutTask(roomId, reservationId);

            ArgumentCaptor<HousekeepingTask> captor = ArgumentCaptor.forClass(HousekeepingTask.class);
            verify(taskRepo).save(captor.capture());
            assertThat(captor.getValue().isCompleted()).isFalse();
        }
    }

    // ══════════════════════════════════════════════════════════════
    // findPendingTasks()
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("findPendingTasks()")
    class FindPendingTasks {

        private final Pageable page = PageRequest.of(0, 20);

        @Test
        @DisplayName("delegates to findPending() and uses countPending() for total")
        void delegatesCorrectly() {
            when(taskRepo.findPending(page)).thenReturn(List.of());
            when(taskRepo.countPending()).thenReturn(5L);

            PageResult<HousekeepingTaskResponse> result = service.findPendingTasks(page);

            assertThat(result.totalElements()).isEqualTo(5L);
            verify(taskRepo).findPending(page);
            verify(taskRepo).countPending();
        }

        @Test
        @DisplayName("returns empty page when no pending tasks exist")
        void emptyWhenNoPendingTasks() {
            when(taskRepo.findPending(page)).thenReturn(List.of());
            when(taskRepo.countPending()).thenReturn(0L);

            PageResult<HousekeepingTaskResponse> result = service.findPendingTasks(page);

            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isZero();
        }

        @Test
        @DisplayName("maps each task to HousekeepingTaskResponse")
        void mapsTasksToResponses() {
            HousekeepingTask task1 = stubPendingTask();
            HousekeepingTask task2 = stubPendingTask();
            when(taskRepo.findPending(page)).thenReturn(List.of(task1, task2));
            when(taskRepo.countPending()).thenReturn(2L);

            PageResult<HousekeepingTaskResponse> result = service.findPendingTasks(page);

            assertThat(result.content()).hasSize(2);
        }
    }

    // ══════════════════════════════════════════════════════════════
    // getBoardByFloor()
    // ══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getBoardByFloor()")
    class GetBoardByFloor {

        @Test
        @DisplayName("returns empty list when no rooms are registered")
        void emptyWhenNoRooms() {
            when(roomStatusPort.getAllRoomsGroupedByFloor()).thenReturn(List.of());

            List<RoomStatusResponse> result = service.getBoardByFloor();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("groups rooms by floor number")
        void groupsByFloor() {
            RoomId roomA = RoomId.generate();
            RoomId roomB = RoomId.generate();
            RoomId roomC = RoomId.generate();
            when(roomStatusPort.getAllRoomsGroupedByFloor()).thenReturn(List.of(
                    new RoomBoardEntry(roomA, "101", 1, "AVAILABLE", "Standard"),
                    new RoomBoardEntry(roomB, "102", 1, "OCCUPIED",  "Deluxe"),
                    new RoomBoardEntry(roomC, "201", 2, "DIRTY",     "Suite")
            ));

            List<RoomStatusResponse> result = service.getBoardByFloor();

            assertThat(result).hasSize(2);
            RoomStatusResponse floor1 = result.stream()
                    .filter(r -> r.floor() == 1).findFirst().orElseThrow();
            assertThat(floor1.rooms()).hasSize(2);
        }

        @Test
        @DisplayName("floors are returned in ascending order (TreeMap guarantee)")
        void floorsAreAscending() {
            when(roomStatusPort.getAllRoomsGroupedByFloor()).thenReturn(List.of(
                    new RoomBoardEntry(RoomId.generate(), "301", 3, "AVAILABLE", "Suite"),
                    new RoomBoardEntry(RoomId.generate(), "101", 1, "AVAILABLE", "Standard"),
                    new RoomBoardEntry(RoomId.generate(), "201", 2, "DIRTY",     "Deluxe")
            ));

            List<RoomStatusResponse> result = service.getBoardByFloor();

            assertThat(result).extracting(RoomStatusResponse::floor)
                    .containsExactly(1, 2, 3);
        }

        @Test
        @DisplayName("single-floor hotel returns one group")
        void singleFloorReturnsSingleGroup() {
            when(roomStatusPort.getAllRoomsGroupedByFloor()).thenReturn(List.of(
                    new RoomBoardEntry(RoomId.generate(), "101", 1, "AVAILABLE", "Standard"),
                    new RoomBoardEntry(RoomId.generate(), "102", 1, "OCCUPIED",  "Standard"),
                    new RoomBoardEntry(RoomId.generate(), "103", 1, "DIRTY",     "Standard")
            ));

            List<RoomStatusResponse> result = service.getBoardByFloor();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).rooms()).hasSize(3);
        }
    }
}
