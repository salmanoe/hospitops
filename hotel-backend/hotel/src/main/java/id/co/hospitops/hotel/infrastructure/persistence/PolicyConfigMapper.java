package id.co.hospitops.hotel.infrastructure.persistence;

import id.co.hospitops.hotel.domain.model.PolicyConfig;
import id.co.hospitops.hotel.infrastructure.persistence.entity.PolicyConfigJpaEntity;
import id.co.hospitops.shared.HotelId;
import id.co.hospitops.shared.PolicyConfigId;
import org.springframework.stereotype.Component;

@Component
class PolicyConfigMapper {

    PolicyConfigJpaEntity toJpa(PolicyConfig p) {
        return PolicyConfigJpaEntity.builder()
                .id(p.getId().value())
                .hotelId(p.getHotelId().value())
                .taxPercent(p.getTaxPercent())
                .taxName(p.getTaxName())
                .invoiceHotelName(p.getInvoiceHotelName())
                .invoiceAddress(p.getInvoiceAddress())
                .invoiceFooterNote(p.getInvoiceFooterNote())
                .build();
    }

    PolicyConfig toDomain(PolicyConfigJpaEntity e) {
        return PolicyConfig.reconstitute(
                PolicyConfigId.of(e.getId()),
                HotelId.of(e.getHotelId()),
                e.getTaxPercent(),
                e.getTaxName(),
                e.getInvoiceHotelName(),
                e.getInvoiceAddress(),
                e.getInvoiceFooterNote(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }
}
