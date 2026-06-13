package id.co.hospitops.hotel.domain;

import id.co.hospitops.hotel.domain.model.PolicyConfig;
import id.co.hospitops.shared.HotelId;
import id.co.hospitops.shared.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PolicyConfig domain")
class PolicyConfigDomainTest {

    private static final HotelId HOTEL_ID = HotelId.generate();

    private PolicyConfig validConfig() {
        return PolicyConfig.create(HOTEL_ID, 11, "PPN",
                "Grand Palace Hotel", "Jl. Sudirman 1, Jakarta", "Thank you for your stay.");
    }

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("creates a valid config with all fields")
        void success() {
            PolicyConfig config = validConfig();

            assertThat(config.getId()).isNotNull();
            assertThat(config.getHotelId()).isEqualTo(HOTEL_ID);
            assertThat(config.getTaxPercent()).isEqualTo(11);
            assertThat(config.getTaxName()).isEqualTo("PPN");
            assertThat(config.getInvoiceHotelName()).isEqualTo("Grand Palace Hotel");
            assertThat(config.getCreatedAt()).isNotNull();
            assertThat(config.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("accepts 0% tax (tax-exempt)")
        void zeroTax() {
            PolicyConfig config = PolicyConfig.create(HOTEL_ID, 0, "Tax-Exempt",
                    "Grand Palace Hotel", null, null);
            assertThat(config.getTaxPercent()).isZero();
        }

        @Test
        @DisplayName("accepts 100% tax (edge case)")
        void maxTax() {
            PolicyConfig config = PolicyConfig.create(HOTEL_ID, 100, "Special Tax",
                    "Grand Palace Hotel", null, null);
            assertThat(config.getTaxPercent()).isEqualTo(100);
        }

        @Test
        @DisplayName("rejects tax percent below 0")
        void negativeTax() {
            assertThatThrownBy(() ->
                    PolicyConfig.create(HOTEL_ID, -1, "PPN", "Hotel", null, null))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("-1");
        }

        @Test
        @DisplayName("rejects tax percent above 100")
        void over100Tax() {
            assertThatThrownBy(() ->
                    PolicyConfig.create(HOTEL_ID, 101, "PPN", "Hotel", null, null))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("101");
        }

        @Test
        @DisplayName("rejects blank tax name")
        void blankTaxName() {
            assertThatThrownBy(() ->
                    PolicyConfig.create(HOTEL_ID, 11, "  ", "Hotel", null, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejects blank invoice hotel name")
        void blankInvoiceHotelName() {
            assertThatThrownBy(() ->
                    PolicyConfig.create(HOTEL_ID, 11, "PPN", "", null, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("trims whitespace from taxName and invoiceHotelName")
        void trimming() {
            PolicyConfig config = PolicyConfig.create(HOTEL_ID, 11, "  PPN  ",
                    "  Grand Palace  ", null, null);
            assertThat(config.getTaxName()).isEqualTo("PPN");
            assertThat(config.getInvoiceHotelName()).isEqualTo("Grand Palace");
        }
    }

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("updates all mutable fields")
        void success() {
            PolicyConfig config = validConfig();

            config.update(7, "VAT", "New Hotel Name", "New Address", "New footer");

            assertThat(config.getTaxPercent()).isEqualTo(7);
            assertThat(config.getTaxName()).isEqualTo("VAT");
            assertThat(config.getInvoiceHotelName()).isEqualTo("New Hotel Name");
            assertThat(config.getInvoiceAddress()).isEqualTo("New Address");
            assertThat(config.getInvoiceFooterNote()).isEqualTo("New footer");
            assertThat(config.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("id and hotelId are immutable after update")
        void immutableFields() {
            PolicyConfig config = validConfig();
            var originalId = config.getId();
            var originalHotelId = config.getHotelId();

            config.update(5, "GST", "Another Hotel", null, null);

            assertThat(config.getId()).isEqualTo(originalId);
            assertThat(config.getHotelId()).isEqualTo(originalHotelId);
        }

        @Test
        @DisplayName("rejects invalid tax percent on update")
        void invalidTaxOnUpdate() {
            PolicyConfig config = validConfig();
            assertThatThrownBy(() -> config.update(150, "PPN", "Hotel", null, null))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }
    }
}
