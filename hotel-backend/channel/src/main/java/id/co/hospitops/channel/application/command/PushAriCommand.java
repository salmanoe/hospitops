package id.co.hospitops.channel.application.command;

import id.co.hospitops.shared.RoomTypeId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Enqueue an ARI push for one room type. Each night carries the availability
 * and rate to publish to the provider.
 */
public record PushAriCommand(RoomTypeId roomTypeId, List<Night> nights) {

    public record Night(LocalDate date, int availability, BigDecimal rate) {
    }
}
