package id.co.hospitops.room.application;

// R-06 FIX: Replaced stub implementation with real availability logic.
// isAvailable() now delegates to the repository's findAvailable() native SQL
// query (which uses a NOT EXISTS subquery on reservations) rather than scanning
// all rooms and filtering in Java — O(1) DB check instead of O(n) scan.
//
// R-18 FIX: findAll() validates the statusFilter before calling RoomStatus.valueOf(),
// throwing a BusinessRuleViolationException on unknown values instead of a raw
// IllegalArgumentException from deep in the JVM stack.

import id.co.hospitops.room.application.command.*;
import id.co.hospitops.room.application.response.*;
import id.co.hospitops.room.domain.model.*;
import id.co.hospitops.room.domain.port.in.*;
import id.co.hospitops.room.domain.port.out.*;
import id.co.hospitops.shared.*;
import id.co.hospitops.shared.HotelContext;
import id.co.hospitops.shared.event.RoomCreatedEvent;
import org.springframework.context.ApplicationEventPublisher;
import id.co.hospitops.shared.exception.*;
import id.co.hospitops.shared.web.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class RoomService implements ManageRoomUseCase, RoomAvailabilityUseCase {

    private final RoomRepository roomRepo;
    private final RoomTypeRepository roomTypeRepo;
    private final RoomRateOverrideRepository overrideRepo;
    private final ApplicationEventPublisher eventPublisher;

    // ── Room ────────────────────────────────────────────────────
    @Override
    public RoomResponse createRoom(CreateRoomCommand cmd) {
        if (roomRepo.existsByRoomNumber(cmd.roomNumber()))
            throw new ConflictException("Room number already exists: " + cmd.roomNumber());
        findRoomType(cmd.roomTypeId()); // validates room type exists

        Room room = Room.create(HotelContext.current(), cmd.roomNumber(), cmd.floor(),
                cmd.roomTypeId(), cmd.notes());
        Room saved = roomRepo.save(room);
        eventPublisher.publishEvent(new RoomCreatedEvent(saved.getHotelId(), saved.getId()));
        RoomType rt = findRoomType(saved.getRoomTypeId());
        return RoomResponse.from(saved, rt);
    }

    @Override
    public RoomResponse updateRoom(RoomId id, UpdateRoomCommand cmd) {
        Room room = findRoom(id);
        room.updateDetails(cmd.floor(), cmd.notes());
        Room saved = roomRepo.save(room);
        return RoomResponse.from(saved, findRoomType(saved.getRoomTypeId()));
    }

    @Override
    @Transactional(readOnly = true)
    public RoomResponse findById(RoomId id) {
        Room room = findRoom(id);
        return RoomResponse.from(room, findRoomType(room.getRoomTypeId()));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<RoomResponse> findAll(String statusFilter, Pageable pageable) {
        List<Room> rooms;
        RoomStatus status = null;

        if (statusFilter != null && !statusFilter.isBlank()) {
            // R-18 FIX: parse enum safely; return HTTP 422 for unknown values.
            // Reuse the parsed value for both the query and the count — no
            // second valueOf() call.
            try {
                status = RoomStatus.valueOf(statusFilter.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BusinessRuleViolationException("Unknown room status: " + statusFilter);
            }
            rooms = roomRepo.findByStatus(status, pageable);
        } else {
            rooms = roomRepo.findAll(pageable);
        }

        List<RoomResponse> list = rooms.stream()
                .map(r -> RoomResponse.from(r, findRoomType(r.getRoomTypeId())))
                .toList();
        long total = (status != null)
                ? roomRepo.countByStatus(status)
                : roomRepo.count();
        // Unpaged.getPageNumber() / getPageSize() throw UnsupportedOperationException.
        // When the caller wants all rows (Pageable.unpaged()), report page 0 and
        // size = total so PageResult math stays coherent.
        int pageNumber = pageable.isPaged() ? pageable.getPageNumber() : 0;
        int pageSize = pageable.isPaged() ? pageable.getPageSize() : (int) total;
        return PageResult.of(list, pageNumber, pageSize, total);
    }

    // ── Availability ────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<AvailableRoomResponse> findAvailable(LocalDate checkIn, LocalDate checkOut) {
        return roomRepo.findAvailable(checkIn, checkOut).stream()
                .map(r -> {
                    RoomType rt = findRoomType(r.getRoomTypeId());
                    List<RoomRateOverride> overrides = overrideRepo.findByRoomTypeId(rt.getId());
                    Money rate = r.resolveRate(checkIn, rt, overrides);
                    return new AvailableRoomResponse(r.getId(), r.getRoomNumber(), r.getFloor(),
                            rt.getId(), rt.getName(), rt.getCapacity(), rate.amount());
                }).toList();
    }

    // R-06 FIX: delegates to a dedicated EXISTS query in the repository —
    // single targeted DB round-trip for one specific room rather than loading
    // all available rooms and filtering in Java (O(1) vs O(n)).
    @Override
    @Transactional(readOnly = true)
    public boolean isAvailable(RoomId roomId, LocalDate checkIn, LocalDate checkOut) {
        return roomRepo.isAvailable(roomId, checkIn, checkOut);
    }

    @Override
    @Transactional(readOnly = true)
    public Money resolveRate(RoomId roomId, LocalDate checkIn) {
        Room room = findRoom(roomId);
        RoomType rt = findRoomType(room.getRoomTypeId());
        List<RoomRateOverride> overrides = overrideRepo.findByRoomTypeId(rt.getId());
        return room.resolveRate(checkIn, rt, overrides);
    }

    // ── Internal status changes (called by domain events) ────────
    @Transactional
    public void markOccupied(RoomId roomId) {
        Room room = findRoom(roomId);
        room.markOccupied();
        roomRepo.save(room);
    }

    @Transactional
    public void markDirty(RoomId roomId) {
        Room room = findRoom(roomId);
        room.markDirty();
        roomRepo.save(room);
    }

    @Transactional
    public void markAvailable(RoomId roomId) {
        Room room = findRoom(roomId);
        room.markAvailable();
        roomRepo.save(room);
    }

    @Override
    @Transactional
    public void changeRoomStatus(RoomId id, RoomStatus newStatus, String notes) {
        Room room = findRoom(id);
        switch (newStatus) {
            case AVAILABLE -> room.markAvailable();
            case MAINTENANCE -> room.markMaintenance(notes != null ? notes : "");
            case SERVICE_REQUESTED -> room.requestService();
            case OCCUPIED -> room.markServiceComplete();
            default -> throw new BusinessRuleViolationException(
                    "Status transition to " + newStatus + " is not permitted via housekeeping endpoint");
        }
        roomRepo.save(room);
    }

    // ── Helpers ─────────────────────────────────────────────────
    private Room findRoom(RoomId id) {
        return roomRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room", id.value()));
    }

    private RoomType findRoomType(RoomTypeId id) {
        return roomTypeRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RoomType", id.value()));
    }
}
