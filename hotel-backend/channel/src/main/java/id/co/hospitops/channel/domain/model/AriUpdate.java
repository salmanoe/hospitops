package id.co.hospitops.channel.domain.model;

import java.time.LocalDate;

/**
 * One night of availability + rate for a provider room type / rate plan.
 * The external ids are the provider's (Channex) identifiers, resolved from
 * the room-type mapping before the message is enqueued.
 *
 * <p>{@code rate} is in integer MINOR units (e.g. cents) — the form Channex
 * accepts on ARI writes. See {@code MinorUnits}.
 */
public record AriUpdate(
        String externalRoomTypeId,
        String externalRatePlanId,
        LocalDate date,
        int availability,
        long rate) {
}
