package id.co.hospitops.reservation.infrastructure.persistence;

import id.co.hospitops.reservation.domain.port.out.ReservationNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Calls PostgreSQL function generate_reservation_number() from V1 schema.
 * Format: RES-YYYY-NNNNN e.g. RES-2025-00042
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationNumberGeneratorImpl implements ReservationNumberGenerator {

    private final JdbcTemplate jdbc;

    @Override
    public String generate() {
        String number = jdbc.queryForObject(
                "SELECT generate_reservation_number()", String.class);
        log.debug("Generated reservation number: {}", number);
        return number;
    }
}
