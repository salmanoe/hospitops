package id.co.hospitops.hotel.application.response;

import id.co.hospitops.hotel.domain.model.PolicyConfig;

import java.time.LocalDateTime;
import java.util.UUID;

public record PolicyConfigResponse(
        UUID id,
        UUID hotelId,
        int taxPercent,
        String taxName,
        String invoiceHotelName,
        String invoiceAddress,
        String invoiceFooterNote,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PolicyConfigResponse from(PolicyConfig p) {
        return new PolicyConfigResponse(
                p.getId().value(),
                p.getHotelId().value(),
                p.getTaxPercent(),
                p.getTaxName(),
                p.getInvoiceHotelName(),
                p.getInvoiceAddress(),
                p.getInvoiceFooterNote(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
