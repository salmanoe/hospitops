package id.co.hospitops.hotel.application;

import id.co.hospitops.hotel.domain.model.HotelSummary;
import id.co.hospitops.hotel.domain.port.out.HotelRepository;
import id.co.hospitops.hotel.domain.port.out.HotelSummaryRepository;
import id.co.hospitops.shared.HotelId;
import id.co.hospitops.shared.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Maintains the {@code hotel_summary} table through incremental updates
 * driven by domain events.
 *
 * <p>Every handler is idempotent in direction (increments/decrements) but NOT in
 * replay — replaying the same event twice will double-count. The nightly
 * {@link HotelSummaryReconciliationJob} is the correctness safety net that resets
 * counts from authoritative table queries.
 *
 * <p>Handlers run in the same transaction as the originating operation (default
 * {@code @EventListener} behaviour), so a handler failure rolls back the whole
 * operation. Use {@code @TransactionalEventListener(AFTER_COMMIT)} if decoupling
 * is needed — but note that would require the summary update to be in its own
 * transaction.
 */
@Slf4j
@Component
@Transactional
@RequiredArgsConstructor
public class HotelSummaryEventHandler {

    private final HotelRepository hotelRepo;
    private final HotelSummaryRepository summaryRepo;

    @EventListener
    public void onRoomCreated(RoomCreatedEvent event) {
        HotelSummary summary = getOrCreate(event.getHotelId());
        summary.incrementTotalRooms();
        summaryRepo.save(summary);
        log.debug("hotel_summary: totalRooms++ for hotel {}", event.getHotelId());
    }

    @EventListener
    public void onReservationCreated(ReservationCreatedEvent event) {
        LocalDate today = LocalDate.now();
        HotelSummary summary = getOrCreate(event.getHotelId());
        if (event.getCheckInDate().equals(today)) {
            summary.incrementArrivals();
        }
        if (event.getCheckOutDate().equals(today)) {
            summary.incrementDepartures();
        }
        summaryRepo.save(summary);
        log.debug("hotel_summary: arrivals/departures updated for hotel {}", event.getHotelId());
    }

    @EventListener
    public void onCheckedIn(ReservationCheckedInEvent event) {
        HotelSummary summary = getOrCreate(event.getHotelId());
        summary.incrementOccupied();
        summaryRepo.save(summary);
        log.debug("hotel_summary: occupiedRooms++ for hotel {}", event.getHotelId());
    }

    @EventListener
    public void onCheckedOut(ReservationCheckedOutEvent event) {
        HotelSummary summary = getOrCreate(event.getHotelId());
        summary.decrementOccupied();
        summaryRepo.save(summary);
        log.debug("hotel_summary: occupiedRooms-- for hotel {}", event.getHotelId());
    }

    @EventListener
    public void onPaymentReceived(PaymentReceivedEvent event) {
        HotelSummary summary = getOrCreate(event.getHotelId());
        summary.addRevenue(event.getAmount());
        summaryRepo.save(summary);
        log.debug("hotel_summary: revenue += {} for hotel {}", event.getAmount(), event.getHotelId());
    }

    @EventListener
    public void onHousekeepingTaskCreated(HousekeepingTaskCreatedEvent event) {
        HotelSummary summary = getOrCreate(event.getHotelId());
        summary.incrementDirtyRooms();
        summaryRepo.save(summary);
        log.debug("hotel_summary: dirtyRooms++ for hotel {}", event.getHotelId());
    }

    // ── When a hotel is created, seed an empty summary row ───────────────────

    @EventListener
    public void onHotelCreated(HotelCreatedEvent event) {
        // Look up the hotel name so the dashboard can display it without a second query.
        // This runs in the same transaction as the hotel insert, so the row is visible.
        String hotelName = hotelRepo.findById(event.getHotelId())
                .map(h -> h.getName())
                .orElse("");
        HotelSummary summary = HotelSummary.empty(event.getHotelId(), hotelName);
        summaryRepo.save(summary);
        log.debug("hotel_summary: seeded empty row for new hotel {}", event.getHotelId());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Loads the summary for the given hotel, creating an empty one if it does not
     * exist yet. An absent row indicates a hotel created before Phase 7 was deployed;
     * the nightly job will recompute it with correct values on its next run.
     */
    private HotelSummary getOrCreate(HotelId hotelId) {
        return summaryRepo.findByHotelId(hotelId)
                .orElseGet(() -> {
                    log.warn("hotel_summary: no row for hotel {} — creating empty placeholder. " +
                            "Nightly reconciliation will correct the counts.", hotelId);
                    // Name will be backfilled by the nightly reconciliation job.
                    return HotelSummary.empty(hotelId, "");
                });
    }
}
