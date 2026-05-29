package id.co.hospitops.hotel.domain;

import id.co.hospitops.hotel.domain.model.*;
import id.co.hospitops.shared.GroupId;
import id.co.hospitops.shared.HotelId;
import id.co.hospitops.shared.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Hotel domain")
class HotelDomainTest {

    // ── Hotel.create() ────────────────────────────────────────────

    @Nested
    @DisplayName("Hotel.create()")
    class HotelCreate {

        @Test
        @DisplayName("starts in SETUP status")
        void startsInSetup() {
            Hotel h = Hotel.create(GroupId.generate(), "Grand Palace");
            assertThat(h.getStatus()).isEqualTo(HotelStatus.SETUP);
        }

        @Test
        @DisplayName("assigns a generated ID")
        void assignsId() {
            Hotel h = Hotel.create(GroupId.generate(), "Grand Palace");
            assertThat(h.getId()).isNotNull();
        }

        @Test
        @DisplayName("checklist is empty on creation")
        void checklistEmpty() {
            Hotel h = Hotel.create(GroupId.generate(), "Grand Palace");
            assertThat(h.getChecklist().isComplete()).isFalse();
            assertThat(h.getChecklist().remainingSteps()).hasSize(5);
        }

        @Test
        @DisplayName("rejects blank name")
        void rejectsBlankName() {
            assertThatThrownBy(() -> Hotel.create(GroupId.generate(), ""))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejects null groupId")
        void rejectsNullGroupId() {
            assertThatThrownBy(() -> Hotel.create(null, "Grand Palace"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ── completeSetupStep() ───────────────────────────────────────

    @Nested
    @DisplayName("completeSetupStep()")
    class CompleteSetupStep {

        @Test
        @DisplayName("does NOT activate before all steps are complete")
        void noActivationBeforeAllSteps() {
            Hotel h = Hotel.create(GroupId.generate(), "Grand Palace");
            boolean activated = h.completeSetupStep(SetupStep.PROFILE);
            assertThat(activated).isFalse();
            assertThat(h.getStatus()).isEqualTo(HotelStatus.SETUP);
        }

        @Test
        @DisplayName("activates when all five steps are completed")
        void activatesWhenAllStepsComplete() {
            Hotel h = Hotel.create(GroupId.generate(), "Grand Palace");
            h.completeSetupStep(SetupStep.PROFILE);
            h.completeSetupStep(SetupStep.POLICY);
            h.completeSetupStep(SetupStep.ROOM_TYPE);
            h.completeSetupStep(SetupStep.ROOM);
            boolean activated = h.completeSetupStep(SetupStep.STAFF_ACCOUNT);

            assertThat(activated).isTrue();
            assertThat(h.getStatus()).isEqualTo(HotelStatus.ACTIVE);
        }

        @Test
        @DisplayName("completing the same step twice is idempotent")
        void idempotentStepCompletion() {
            Hotel h = Hotel.create(GroupId.generate(), "Grand Palace");
            h.completeSetupStep(SetupStep.PROFILE);
            boolean activatedAgain = h.completeSetupStep(SetupStep.PROFILE);
            assertThat(activatedAgain).isFalse();
            assertThat(h.getChecklist().remainingSteps()).hasSize(4);
        }

        @Test
        @DisplayName("throws when called on a non-SETUP hotel")
        void throwsWhenNotInSetup() {
            Hotel h = completeSetupWizard();
            // now ACTIVE
            assertThatThrownBy(() -> h.completeSetupStep(SetupStep.PROFILE))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }
    }

    // ── suspend() / reactivate() ──────────────────────────────────

    @Nested
    @DisplayName("suspend()")
    class Suspend {

        @Test
        @DisplayName("transitions ACTIVE → SUSPENDED")
        void suspends() {
            Hotel h = completeSetupWizard();
            h.suspend();
            assertThat(h.getStatus()).isEqualTo(HotelStatus.SUSPENDED);
        }

        @Test
        @DisplayName("throws when called on a SETUP hotel")
        void throwsOnSetupHotel() {
            Hotel h = Hotel.create(GroupId.generate(), "Grand Palace");
            assertThatThrownBy(h::suspend)
                    .isInstanceOf(BusinessRuleViolationException.class);
        }

        @Test
        @DisplayName("throws when called on an already SUSPENDED hotel")
        void throwsOnAlreadySuspended() {
            Hotel h = completeSetupWizard();
            h.suspend();
            assertThatThrownBy(h::suspend)
                    .isInstanceOf(BusinessRuleViolationException.class);
        }
    }

    @Nested
    @DisplayName("reactivate()")
    class Reactivate {

        @Test
        @DisplayName("transitions SUSPENDED → ACTIVE")
        void reactivates() {
            Hotel h = completeSetupWizard();
            h.suspend();
            h.reactivate();
            assertThat(h.getStatus()).isEqualTo(HotelStatus.ACTIVE);
        }

        @Test
        @DisplayName("throws when called on an ACTIVE hotel")
        void throwsOnActiveHotel() {
            Hotel h = completeSetupWizard();
            assertThatThrownBy(h::reactivate)
                    .isInstanceOf(BusinessRuleViolationException.class);
        }

        @Test
        @DisplayName("throws when called on a SETUP hotel")
        void throwsOnSetupHotel() {
            Hotel h = Hotel.create(GroupId.generate(), "Grand Palace");
            assertThatThrownBy(h::reactivate)
                    .isInstanceOf(BusinessRuleViolationException.class);
        }
    }

    // ── SetupChecklist ────────────────────────────────────────────

    @Nested
    @DisplayName("SetupChecklist")
    class SetupChecklistTests {

        @Test
        @DisplayName("empty checklist has all five remaining steps")
        void emptyChecklistHasFiveSteps() {
            SetupChecklist c = SetupChecklist.empty(HotelId.generate());
            assertThat(c.remainingSteps()).containsExactlyInAnyOrder(SetupStep.values());
        }

        @Test
        @DisplayName("completing a step removes it from remainingSteps")
        void completingRemovesStep() {
            SetupChecklist c = SetupChecklist.empty(HotelId.generate());
            c.complete(SetupStep.PROFILE);
            assertThat(c.remainingSteps()).doesNotContain(SetupStep.PROFILE);
        }

        @Test
        @DisplayName("isComplete returns true only when all five are done")
        void isCompleteRequiresAllFive() {
            SetupChecklist c = SetupChecklist.empty(HotelId.generate());
            for (SetupStep step : SetupStep.values()) {
                assertThat(c.isComplete()).isFalse();
                c.complete(step);
            }
            assertThat(c.isComplete()).isTrue();
        }
    }

    // ── helpers ───────────────────────────────────────────────────

    private Hotel completeSetupWizard() {
        Hotel h = Hotel.create(GroupId.generate(), "Grand Palace");
        h.completeSetupStep(SetupStep.PROFILE);
        h.completeSetupStep(SetupStep.POLICY);
        h.completeSetupStep(SetupStep.ROOM_TYPE);
        h.completeSetupStep(SetupStep.ROOM);
        h.completeSetupStep(SetupStep.STAFF_ACCOUNT);
        return h;
    }
}
