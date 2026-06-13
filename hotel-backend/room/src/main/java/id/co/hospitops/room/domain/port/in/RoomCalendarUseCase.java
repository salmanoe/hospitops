package id.co.hospitops.room.domain.port.in;

import id.co.hospitops.room.application.response.RoomCalendarResponse;

import java.time.LocalDate;
import java.util.List;

/** Availability + rate grid for the rate calendar, over a bounded date range. */
public interface RoomCalendarUseCase {
    List<RoomCalendarResponse> calendar(LocalDate from, LocalDate to);
}
