package id.co.hospitops.shared;

import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Contract tests for {@link HotelContext}.
 *
 * <p>Verifies that the {@link ScopedValue} slot is correctly bound and unbound,
 * that {@link HotelContext#current()} fails fast when no context is present,
 * and that {@link HotelContext#isBound()} reflects the binding state accurately.
 */
@DisplayName("HotelContext")
class HotelContextTest {

    // ── isBound ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isBound()")
    class IsBound {

        @Test
        @DisplayName("returns false when no hotel context is bound")
        void returnsFalseWhenUnbound() {
            assertThat(HotelContext.isBound()).isFalse();
        }

        @Test
        @DisplayName("returns true inside a ScopedValue binding")
        void returnsTrueWhenBound() throws Exception {
            HotelId hotelId = HotelId.generate();

            ScopedValue.where(HotelContext.HOTEL_ID, hotelId).call(() -> {
                assertThat(HotelContext.isBound()).isTrue();
                return null;
            });
        }

        @Test
        @DisplayName("returns false again after the ScopedValue scope exits")
        void returnsFalseAfterScopeExits() throws Exception {
            HotelId hotelId = HotelId.generate();

            ScopedValue.where(HotelContext.HOTEL_ID, hotelId).call(() -> null);

            assertThat(HotelContext.isBound()).isFalse();
        }
    }

    // ── current() ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("current()")
    class Current {

        @Test
        @DisplayName("throws IllegalStateException when no hotel context is bound")
        void throwsWhenUnbound() {
            assertThatThrownBy(HotelContext::current)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No hotel context bound");
        }

        @Test
        @DisplayName("returns the bound HotelId inside a ScopedValue binding")
        void returnsHotelIdWhenBound() throws Exception {
            HotelId hotelId = HotelId.generate();

            ScopedValue.where(HotelContext.HOTEL_ID, hotelId).call(() -> {
                assertThat(HotelContext.current()).isEqualTo(hotelId);
                return null;
            });
        }

        @Test
        @DisplayName("inner binding shadows the outer binding")
        void innerBindingShadowsOuter() throws Exception {
            HotelId outer = HotelId.generate();
            HotelId inner = HotelId.generate();

            ScopedValue.where(HotelContext.HOTEL_ID, outer).call(() ->
                ScopedValue.where(HotelContext.HOTEL_ID, inner).call(() -> {
                    assertThat(HotelContext.current()).isEqualTo(inner);
                    return null;
                })
            );
        }

        @Test
        @DisplayName("outer binding is restored after inner scope exits")
        void outerRestoredAfterInnerScopeExits() throws Exception {
            HotelId outer = HotelId.generate();
            HotelId inner = HotelId.generate();

            ScopedValue.where(HotelContext.HOTEL_ID, outer).call(() -> {
                ScopedValue.where(HotelContext.HOTEL_ID, inner).call(() -> null);
                assertThat(HotelContext.current()).isEqualTo(outer);
                return null;
            });
        }
    }

    // ── utility class invariants ──────────────────────────────────────────

    @Nested
    @DisplayName("utility class invariants")
    class UtilityClassInvariants {

        @Test
        @DisplayName("is declared final")
        void isFinal() {
            assertThat(HotelContext.class).isFinal();
        }

        @Test
        @DisplayName("has exactly one private constructor")
        void hasSinglePrivateConstructor() {
            var constructors = HotelContext.class.getDeclaredConstructors();
            assertThat(constructors).hasSize(1);
            assertThat(java.lang.reflect.Modifier.isPrivate(constructors[0].getModifiers())).isTrue();
        }
    }
}
