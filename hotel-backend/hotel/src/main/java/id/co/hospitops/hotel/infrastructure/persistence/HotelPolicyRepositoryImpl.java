package id.co.hospitops.hotel.infrastructure.persistence;

import id.co.hospitops.hotel.domain.model.PolicyConfig;
import id.co.hospitops.hotel.domain.port.out.HotelPolicyConfigRepository;
import id.co.hospitops.hotel.infrastructure.persistence.entity.PolicyConfigJpaEntity;
import id.co.hospitops.shared.HotelId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
class HotelPolicyRepositoryImpl implements HotelPolicyConfigRepository {

    private final PolicyConfigJpaRepository jpa;
    private final PolicyConfigMapper mapper;

    @Override
    public PolicyConfig save(PolicyConfig config) {
        PolicyConfigJpaEntity entity = jpa.findByHotelId(config.getHotelId().value())
                .map(existing -> updateFields(existing, config))
                .orElseGet(() -> mapper.toJpa(config));
        return mapper.toDomain(jpa.save(entity));
    }

    @Override
    public Optional<PolicyConfig> findByHotelId(HotelId hotelId) {
        return jpa.findByHotelId(hotelId.value()).map(mapper::toDomain);
    }

    @Override
    public boolean existsByHotelId(HotelId hotelId) {
        return jpa.existsByHotelId(hotelId.value());
    }

    private PolicyConfigJpaEntity updateFields(PolicyConfigJpaEntity entity, PolicyConfig config) {
        entity.setTaxPercent(config.getTaxPercent());
        entity.setTaxName(config.getTaxName());
        entity.setInvoiceHotelName(config.getInvoiceHotelName());
        entity.setInvoiceAddress(config.getInvoiceAddress());
        entity.setInvoiceFooterNote(config.getInvoiceFooterNote());
        return entity;
    }
}
