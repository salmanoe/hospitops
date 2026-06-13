package id.co.hospitops.channel.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One night of availability + rate for a provider room type / rate plan.
 * The external ids are the provider's (Channex) identifiers, resolved from
 * the room-type mapping before the message is enqueued.
 */
public record AriUpdate(
        String externalRoomTypeId,
        String externalRatePlanId,
        LocalDate date,
        int availability,
        BigDecimal rate) {
}
