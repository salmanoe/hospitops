package id.co.hospitops.room.domain;

import id.co.hospitops.room.domain.model.RoomType;
import id.co.hospitops.shared.HotelId;
import id.co.hospitops.shared.Money;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for RoomType.validate() — covers the R-10 bug fix.
 * <p>
 * The original condition was always false:
 * basePrice.isZero() && !basePrice.amount().equals(BigDecimal.ZERO)
 * so negative prices were silently accepted. The fix uses compareTo < 0
 * to correctly reject negative prices while allowing zero.
 */
@DisplayName("RoomType validation")
class RoomTypeTest {

    @Test
    @DisplayName("creates room type with valid inputs")
    void createsWithValidInputs() {
        RoomType rt = RoomType.create(HotelId.generate(), "Deluxe", 2, "Sea view", Money.of(500_000L));
        assertThat(rt.getName()).isEqualTo("Deluxe");
        assertThat(rt.getCapacity()).isEqualTo(2);
        assertThat(rt.getBasePrice().amount()).isEqualByComparingTo(BigDecimal.valueOf(500_000));
    }

    @Test
    @DisplayName("zero base price is allowed (complimentary room type)")
    void zeroPriceAllowed() {
        assertThatNoException().isThrownBy(() ->
                RoomType.create(HotelId.generate(), "Staff", 1, "Internal use", Money.of(0L)));
    }

    @Test
    @DisplayName("negative base price throws (R-10 fix)")
    void negativePriceThrows() {
        assertThatThrownBy(() ->
                RoomType.create(HotelId.generate(), "Deluxe", 2, "desc", Money.of(BigDecimal.valueOf(-1))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negative");
    }

    @Test
    @DisplayName("null base price throws")
    void nullPriceThrows() {
        assertThatThrownBy(() ->
                RoomType.create(HotelId.generate(), "Deluxe", 2, "desc", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("blank name throws")
    void blankNameThrows() {
        assertThatThrownBy(() ->
                RoomType.create(HotelId.generate(), "  ", 2, "desc", Money.of(100_000L)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("capacity < 1 throws")
    void zeroCapacityThrows() {
        assertThatThrownBy(() ->
                RoomType.create(HotelId.generate(), "Deluxe", 0, "desc", Money.of(100_000L)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("update() applies same validation rules")
    void updateValidatesTheSameWay() {
        RoomType rt = RoomType.create(HotelId.generate(), "Deluxe", 2, "desc", Money.of(500_000L));
        assertThatThrownBy(() ->
                rt.update("Deluxe", 2, "desc", Money.of(BigDecimal.valueOf(-500))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negative");
    }
}
