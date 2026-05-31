package id.co.hospitops.bootstrap;

import id.co.hospitops.billing.domain.port.out.HotelPolicyPort;
import id.co.hospitops.hotel.infrastructure.persistence.PolicyConfigJpaRepository;
import id.co.hospitops.hotel.infrastructure.persistence.entity.PolicyConfigJpaEntity;
import id.co.hospitops.shared.HotelId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Implements billing's {@link HotelPolicyPort} by reading directly from the
 * {@code hotel_policy_config} table via the {@code hotel} module's
 * {@link PolicyConfigJpaRepository}.
 *
 * <p>Cross-module repository access is a Stage 1 monolith pattern documented
 * as technical debt in {@code V19__create_hotel_policy_config_table.sql}.
 * In Stage 3 (microservices), this becomes an inter-service HTTP or event call.
 *
 * <p>Falls back to safe defaults when no policy has been configured for a hotel —
 * this keeps invoice generation working for hotels that existed before Phase 9.
 * The V19 migration seeds a default row for the initial hotel to cover real data.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HotelPolicyAdapter implements HotelPolicyPort {

    private static final HotelPolicy DEFAULT_POLICY = new HotelPolicy(
            "HospitOps Hotel",
            null,
            "Thank you for staying with us. We look forward to welcoming you again.",
            11,
            "PPN"
    );

    private final PolicyConfigJpaRepository policyConfigJpaRepository;

    @Override
    public HotelPolicy findByHotelId(HotelId hotelId) {
        Optional<PolicyConfigJpaEntity> entity =
                policyConfigJpaRepository.findByHotelId(hotelId.value());

        if (entity.isEmpty()) {
            log.warn("No policy config found for hotel {} — using defaults", hotelId);
            return DEFAULT_POLICY;
        }

        PolicyConfigJpaEntity e = entity.get();
        return new HotelPolicy(
                e.getInvoiceHotelName(),
                e.getInvoiceAddress(),
                e.getInvoiceFooterNote(),
                e.getTaxPercent(),
                e.getTaxName()
        );
    }
}
