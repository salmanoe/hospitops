package id.co.hospitops.billing.infrastructure.persistence;

import id.co.hospitops.billing.domain.port.out.InvoiceNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Calls the PostgreSQL sequence function defined in V1__init_schema.sql.
 * Format: INV-YYYY-NNNNN  e.g. INV-2025-00001
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InvoiceNumberGeneratorImpl implements InvoiceNumberGenerator {

    private final JdbcTemplate jdbc;

    @Override
    public String generate() {
        String number = jdbc.queryForObject(
                "SELECT generate_invoice_number()", String.class);
        log.debug("Generated invoice number: {}", number);
        return number;
    }
}
