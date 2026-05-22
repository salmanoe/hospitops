package id.co.hospitops.billing.domain.port.out;

public interface InvoiceNumberGenerator {
    /**
     * Calls the DB sequence function generate_invoice_number() → INV-YYYY-NNNNN
     */
    String generate();
}
