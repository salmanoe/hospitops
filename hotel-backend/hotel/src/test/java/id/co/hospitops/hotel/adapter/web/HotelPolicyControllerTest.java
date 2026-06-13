package id.co.hospitops.hotel.adapter.web;

import id.co.hospitops.hotel.application.response.PolicyConfigResponse;
import id.co.hospitops.hotel.domain.port.in.ManageHotelPolicyUseCase;
import id.co.hospitops.hotel.domain.port.in.ManageHotelUseCase;
import id.co.hospitops.hotel.domain.port.in.GroupDashboardUseCase;
import id.co.hospitops.shared.HotelId;
import id.co.hospitops.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HotelPolicyController.class)
@DisplayName("HotelPolicyController")
class HotelPolicyControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean ManageHotelPolicyUseCase policyUseCase;
    // Siblings in the same scan scope — mock their dependencies
    @MockitoBean ManageHotelUseCase hotelUseCase;
    @MockitoBean GroupDashboardUseCase groupDashboardUseCase;

    private static final UUID HOTEL_UUID = UUID.randomUUID();

    @BeforeEach
    void setUpSecurityContext() {
        // No principal needed for these endpoints (hotelId comes from path, not principal)
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private PolicyConfigResponse response(int taxPercent) {
        return new PolicyConfigResponse(
                UUID.randomUUID(), HOTEL_UUID,
                taxPercent, "PPN",
                "Grand Palace Hotel", "Jl. Sudirman 1", "Thank you for your stay.",
                LocalDateTime.now(), LocalDateTime.now());
    }

    private String validBody() {
        return """
                {
                  "taxPercent": 11,
                  "taxName": "PPN",
                  "invoiceHotelName": "Grand Palace Hotel",
                  "invoiceAddress": "Jl. Sudirman 1",
                  "invoiceFooterNote": "Thank you for your stay."
                }
                """;
    }

    // ── PUT /api/v1/group/hotels/{hotelId}/policy ─────────────────────

    @Nested
    @DisplayName("PUT /api/v1/group/hotels/{hotelId}/policy")
    class SavePolicy {

        @Test
        @DisplayName("returns 200 with saved config")
        void success() throws Exception {
            given(policyUseCase.savePolicyConfig(argThat(
                    cmd -> cmd.taxPercent() == 11 && "PPN".equals(cmd.taxName()))))
                    .willReturn(response(11));

            mockMvc.perform(put("/api/v1/group/hotels/{id}/policy", HOTEL_UUID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validBody()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.taxPercent").value(11))
                    .andExpect(jsonPath("$.data.taxName").value("PPN"))
                    .andExpect(jsonPath("$.data.invoiceHotelName").value("Grand Palace Hotel"));
        }

        @Test
        @DisplayName("passes the correct hotelId from path to the use case")
        void passesHotelId() throws Exception {
            given(policyUseCase.savePolicyConfig(any())).willReturn(response(11));

            mockMvc.perform(put("/api/v1/group/hotels/{id}/policy", HOTEL_UUID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validBody()))
                    .andExpect(status().isOk());

            then(policyUseCase).should().savePolicyConfig(
                    argThat(cmd -> HOTEL_UUID.equals(cmd.hotelId().value())));
        }

        @Test
        @DisplayName("returns 400 when taxPercent is negative")
        void negativeTax() throws Exception {
            mockMvc.perform(put("/api/v1/group/hotels/{id}/policy", HOTEL_UUID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"taxPercent":-1,"taxName":"PPN",
                                     "invoiceHotelName":"Grand Palace"}
                                    """))
                    .andExpect(status().isBadRequest());
            then(policyUseCase).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("returns 400 when taxPercent exceeds 100")
        void taxOver100() throws Exception {
            mockMvc.perform(put("/api/v1/group/hotels/{id}/policy", HOTEL_UUID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"taxPercent":101,"taxName":"PPN",
                                     "invoiceHotelName":"Grand Palace"}
                                    """))
                    .andExpect(status().isBadRequest());
            then(policyUseCase).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("returns 400 when taxName is blank")
        void blankTaxName() throws Exception {
            mockMvc.perform(put("/api/v1/group/hotels/{id}/policy", HOTEL_UUID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"taxPercent":11,"taxName":"",
                                     "invoiceHotelName":"Grand Palace"}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when invoiceHotelName is blank")
        void blankInvoiceHotelName() throws Exception {
            mockMvc.perform(put("/api/v1/group/hotels/{id}/policy", HOTEL_UUID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"taxPercent":11,"taxName":"PPN",
                                     "invoiceHotelName":""}
                                    """))
                    .andExpect(status().isBadRequest());
        }
    }

    // ── GET /api/v1/group/hotels/{hotelId}/policy ─────────────────────

    @Nested
    @DisplayName("GET /api/v1/group/hotels/{hotelId}/policy")
    class GetPolicy {

        @Test
        @DisplayName("returns 200 with config when present")
        void found() throws Exception {
            given(policyUseCase.findByHotelId(HotelId.of(HOTEL_UUID)))
                    .willReturn(response(11));

            mockMvc.perform(get("/api/v1/group/hotels/{id}/policy", HOTEL_UUID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.hotelId").value(HOTEL_UUID.toString()))
                    .andExpect(jsonPath("$.data.taxPercent").value(11));
        }

        @Test
        @DisplayName("returns 404 when no policy has been saved")
        void notFound() throws Exception {
            given(policyUseCase.findByHotelId(any()))
                    .willThrow(new ResourceNotFoundException("PolicyConfig", HOTEL_UUID));

            mockMvc.perform(get("/api/v1/group/hotels/{id}/policy", HOTEL_UUID))
                    .andExpect(status().isNotFound());
        }
    }
}
