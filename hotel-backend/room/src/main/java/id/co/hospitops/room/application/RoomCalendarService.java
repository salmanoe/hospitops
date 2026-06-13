package id.co.hospitops.room.application;

import id.co.hospitops.room.application.response.RoomCalendarResponse;
import id.co.hospitops.room.domain.model.RoomType;
import id.co.hospitops.room.domain.port.in.RoomCalendarUseCase;
import id.co.hospitops.room.domain.port.out.RoomTypeRepository;
import id.co.hospitops.shared.channel.RoomAvailabilitySnapshotProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the rate/availability calendar by combining each room type's sellable
 * count and effective rate per night (reusing {@link RoomAvailabilitySnapshotProvider},
 * which already resolves overrides and bookings). The range is capped so the
 * grid stays a bounded read.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RoomCalendarService implements RoomCalendarUseCase {

    private static final int MAX_DAYS = 62;

    private final RoomTypeRepository roomTypeRepo;
    private final RoomAvailabilitySnapshotProvider snapshot;

    @Override
    public List<RoomCalendarResponse> calendar(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) {
            return List.of();
        }
        LocalDate cap = from.plusDays(MAX_DAYS);
        LocalDate end = to.isAfter(cap) ? cap : to;

        List<RoomCalendarResponse> result = new ArrayList<>();
        for (RoomType type : roomTypeRepo.findAll(PageRequest.of(0, 200))) {
            List<RoomCalendarResponse.DayCell> days = new ArrayList<>();
            for (LocalDate d = from; !d.isAfter(end); d = d.plusDays(1)) {
                days.add(new RoomCalendarResponse.DayCell(
                        d,
                        snapshot.availableUnits(type.getId(), d),
                        snapshot.ratePerNight(type.getId(), d).amount()));
            }
            result.add(new RoomCalendarResponse(type.getId(), type.getName(), type.getCapacity(), days));
        }
        return result;
    }
}
