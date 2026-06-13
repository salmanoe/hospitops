package id.co.hospitops.channel.domain.model;

/**
 * Outcome of processing an inbound OTA booking.
 *
 * <ul>
 *   <li>{@code BOOKED}   — a HospitOps reservation was created.</li>
 *   <li>{@code CANCELLED}— the booking was cancelled on the OTA side.</li>
 *   <li>{@code CONFLICT} — could not be applied (no room available / room type
 *       not mapped); needs staff attention. Still acked so it stops re-serving.</li>
 * </ul>
 */
public enum InboundStatus {
    BOOKED,
    CANCELLED,
    CONFLICT
}
