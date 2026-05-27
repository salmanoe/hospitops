package id.co.hospitops.room.domain;

import id.co.hospitops.room.domain.model.RoomRateOverride;
import id.co.hospitops.shared.Money;
import id.co.hospitops.shared.RoomTypeId;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for the RoomRateOverride record.
 *
 * Covers the compact-constructor validUntil >= validFrom guard
 * and the isActiveOn() date-range predicate.
 */
@DisplayName("RoomRateOverride")
class RoomRateOverrideTest {

    private static final LocalDate TODAY     = LocalDate.of(2025, 6, 1);
    private static final LocalDate YESTERDAY = TODAY.minusDays(1);
    private static final LocalDate TOMORROW  = TODAY.plusDays(1);
    private static final LocalDate NEXT_WEEK = TODAY.plusDays(7);

    private RoomRateOverride override(LocalDate from, LocalDate until) {
        return new RoomRateOverride(UUID.randomUUID(), RoomTypeId.generate(),
                "Weekend Rate", Money.of(700_000L), from, until);
    }

    // ── compact constructor guard ────────────────────────────────────────

    @Nested
    @DisplayName("compact constructor")
    class Constructor {

        @Test
        @DisplayName("constructs successfully when validUntil == validFrom (single day)")
        void singleDayRangeIsAllowed() {
            assertThatNoException().isThrownBy(() -> override(TODAY, TODAY));
        }

        @Test
        @DisplayName("constructs successfully when validUntil > validFrom")
        void normalRangeIsAllowed() {
            assertThatNoException().isThrownBy(() -> override(TODAY, NEXT_WEEK));
        }

        @Test
        @DisplayName("throws when validUntil is before validFrom")
        void throwsWhenUntilBeforeFrom() {
            assertThatThrownBy(() -> override(TODAY, YESTERDAY))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("validUntil");
        }
    }

    // ── isActiveOn ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("isActiveOn()")
    class IsActiveOn {

        @Test
        @DisplayName("returns true for a date on the first day of the range")
        void trueOnFirstDay() {
            assertThat(override(TODAY, NEXT_WEEK).isActiveOn(TODAY)).isTrue();
        }

        @Test
        @DisplayName("returns true for a date on the last day of the range")
        void trueOnLastDay() {
            assertThat(override(TODAY, NEXT_WEEK).isActiveOn(NEXT_WEEK)).isTrue();
        }

        @Test
        @DisplayName("returns true for a date in the middle of the range")
        void trueInMiddleOfRange() {
            assertThat(override(TODAY, NEXT_WEEK).isActiveOn(TOMORROW)).isTrue();
        }

        @Test
        @DisplayName("returns false for a date before the range starts")
        void falseBeforeRange() {
            assertThat(override(TODAY, NEXT_WEEK).isActiveOn(YESTERDAY)).isFalse();
        }

        @Test
        @DisplayName("returns false for a date after the range ends")
        void falseAfterRange() {
            LocalDate afterEnd = NEXT_WEEK.plusDays(1);
            assertThat(override(TODAY, NEXT_WEEK).isActiveOn(afterEnd)).isFalse();
        }

        @Test
        @DisplayName("single-day range: active only on that exact day")
        void singleDayRangeActiveOnlyThatDay() {
            RoomRateOverride singleDay = override(TODAY, TODAY);
            assertThat(singleDay.isActiveOn(TODAY)).isTrue();
            assertThat(singleDay.isActiveOn(YESTERDAY)).isFalse();
            assertThat(singleDay.isActiveOn(TOMORROW)).isFalse();
        }
    }
}
