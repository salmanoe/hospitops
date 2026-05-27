package id.co.hospitops.shared;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Guard")
class GuardTest {

    // ── notNull ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("notNull()")
    class NotNull {

        @Test
        @DisplayName("returns the value when non-null")
        void returnsNonNullValue() {
            String value = "hello";
            assertThat(Guard.notNull(value, "field")).isSameAs(value);
        }

        @Test
        @DisplayName("throws for null value")
        void throwsForNull() {
            assertThatThrownBy(() -> Guard.notNull(null, "myField"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("myField")
                    .hasMessageContaining("must not be null");
        }

        @Test
        @DisplayName("works for non-String types")
        void worksForNonStringTypes() {
            Integer num = 42;
            assertThat(Guard.notNull(num, "num")).isEqualTo(42);
        }
    }

    // ── notBlank ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("notBlank()")
    class NotBlank {

        @Test
        @DisplayName("returns the value when non-blank")
        void returnsNonBlankValue() {
            assertThat(Guard.notBlank("hello", "field")).isEqualTo("hello");
        }

        @Test
        @DisplayName("throws for null")
        void throwsForNull() {
            assertThatThrownBy(() -> Guard.notBlank(null, "field"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("field");
        }

        @ParameterizedTest(name = "value = \"{0}\"")
        @ValueSource(strings = {"", " ", "   ", "\t", "\n"})
        @DisplayName("throws for blank strings")
        void throwsForBlankStrings(String blank) {
            assertThatThrownBy(() -> Guard.notBlank(blank, "field"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not be blank");
        }
    }

    // ── maxLength ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("maxLength()")
    class MaxLength {

        @Test
        @DisplayName("returns value when within limit")
        void returnsValueWithinLimit() {
            assertThat(Guard.maxLength("hello", 10, "field")).isEqualTo("hello");
        }

        @Test
        @DisplayName("returns value at exactly the limit")
        void returnsValueAtLimit() {
            String exactly10 = "1234567890";
            assertThat(Guard.maxLength(exactly10, 10, "field")).isEqualTo(exactly10);
        }

        @Test
        @DisplayName("throws when value exceeds the limit")
        void throwsWhenExceedsLimit() {
            String tooLong = "A".repeat(201);
            assertThatThrownBy(() -> Guard.maxLength(tooLong, 200, "name"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name")
                    .hasMessageContaining("200");
        }

        @Test
        @DisplayName("throws for null value")
        void throwsForNull() {
            assertThatThrownBy(() -> Guard.maxLength(null, 10, "field"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ── positive(int) ────────────────────────────────────────────────────

    @Nested
    @DisplayName("positive(int)")
    class PositiveInt {

        @Test
        @DisplayName("returns value when positive")
        void returnsPositiveValue() {
            assertThat(Guard.positive(1, "field")).isEqualTo(1);
            assertThat(Guard.positive(100, "field")).isEqualTo(100);
        }

        @Test
        @DisplayName("throws for zero")
        void throwsForZero() {
            assertThatThrownBy(() -> Guard.positive(0, "capacity"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("capacity")
                    .hasMessageContaining("positive");
        }

        @Test
        @DisplayName("throws for negative value")
        void throwsForNegative() {
            assertThatThrownBy(() -> Guard.positive(-1, "floor"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("floor");
        }
    }

    // ── nonNegative ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("nonNegative()")
    class NonNegative {

        @Test
        @DisplayName("returns zero")
        void returnsZero() {
            assertThat(Guard.nonNegative(0, "children")).isEqualTo(0);
        }

        @Test
        @DisplayName("returns positive value")
        void returnsPositive() {
            assertThat(Guard.nonNegative(5, "field")).isEqualTo(5);
        }

        @Test
        @DisplayName("throws for negative value")
        void throwsForNegative() {
            assertThatThrownBy(() -> Guard.nonNegative(-1, "count"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("count")
                    .hasMessageContaining(">= 0");
        }
    }

    // ── positive(long) ───────────────────────────────────────────────────

    @Nested
    @DisplayName("positive(long)")
    class PositiveLong {

        @Test
        @DisplayName("returns positive long value")
        void returnsPositiveLong() {
            assertThat(Guard.positive(1L, "nights")).isEqualTo(1L);
        }

        @Test
        @DisplayName("throws for zero long")
        void throwsForZeroLong() {
            assertThatThrownBy(() -> Guard.positive(0L, "nights"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("nights");
        }

        @Test
        @DisplayName("throws for negative long")
        void throwsForNegativeLong() {
            assertThatThrownBy(() -> Guard.positive(-5L, "nights"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ── min ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("min()")
    class Min {

        @Test
        @DisplayName("returns value equal to minimum")
        void returnsValueAtMinimum() {
            assertThat(Guard.min(5, 5, "field")).isEqualTo(5);
        }

        @Test
        @DisplayName("returns value above minimum")
        void returnsValueAboveMinimum() {
            assertThat(Guard.min(10, 5, "field")).isEqualTo(10);
        }

        @Test
        @DisplayName("throws when value is below minimum")
        void throwsWhenBelowMinimum() {
            assertThatThrownBy(() -> Guard.min(3, 5, "price"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("price")
                    .hasMessageContaining(">= 5");
        }

        @Test
        @DisplayName("throws for null value")
        void throwsForNull() {
            assertThatThrownBy(() -> Guard.min(null, 0, "field"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ── isTrue ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isTrue()")
    class IsTrue {

        @Test
        @DisplayName("passes silently for a true condition")
        void passesForTrueCondition() {
            assertThatNoException().isThrownBy(() -> Guard.isTrue(true, "should be fine"));
        }

        @Test
        @DisplayName("throws with the provided message for a false condition")
        void throwsForFalseCondition() {
            assertThatThrownBy(() -> Guard.isTrue(false, "checkout must be after checkin"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("checkout must be after checkin");
        }
    }
}
