package id.co.hospitops.reservation.adapter.web;

// R-14 FIX: Removed direct import of id.co.hospitops.identity.domain.model.Staff.
// The reservation module must not have a compile-time dependency on the identity
// domain model. Instead, we use @AuthenticationPrincipal with an expression to
// extract only the StaffId (a shared-kernel type) from the security principal,
// keeping the module boundary clean.

import id.co.hospitops.reservation.application.command.CreateReservationCommand;
import id.co.hospitops.reservation.application.response.ReservationResponse;
import id.co.hospitops.reservation.domain.port.in.ReservationUseCase;
import id.co.hospitops.reservation.adapter.web.request.CreateReservationRequest;
import id.co.hospitops.shared.*;
import id.co.hospitops.shared.web.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequiresHotelContext
@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationUseCase reservationUseCase;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResult<ReservationResponse>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(reservationUseCase.findAll(status,
                PageRequest.of(page, size, Sort.by("checkInDate").descending()))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReservationResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(
                reservationUseCase.findById(ReservationId.of(id))));
    }

    @GetMapping("/today/arrivals")
    public ResponseEntity<ApiResponse<List<ReservationResponse>>> arrivals() {
        return ResponseEntity.ok(ApiResponse.ok(reservationUseCase.todayArrivals()));
    }

    @GetMapping("/today/departures")
    public ResponseEntity<ApiResponse<List<ReservationResponse>>> departures() {
        return ResponseEntity.ok(ApiResponse.ok(reservationUseCase.todayDepartures()));
    }

    @PostMapping
    @SuppressWarnings({"JvmTaintAnalysis", "SpringElInspection"})
    // @Valid + Bean Validation; JSON response; SpEL 'id' resolves at runtime on the Staff principal
    public ResponseEntity<ApiResponse<ReservationResponse>> create(
            @Valid @RequestBody CreateReservationRequest req,
            // R-14 FIX: expression = "id" extracts only the StaffId field from
            // the authenticated Staff principal — no cross-module import needed.
            @AuthenticationPrincipal(expression = "id") StaffId staffId) {
        var cmd = new CreateReservationCommand(
                GuestId.of(req.guestId()), RoomId.of(req.roomId()),
                req.checkIn(), req.checkOut(), req.adults(), req.children(),
                req.specialRequests(), staffId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(reservationUseCase.create(cmd)));
    }

    @PatchMapping("/{id}/checkin")
    public ResponseEntity<ApiResponse<ReservationResponse>> checkIn(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(
                reservationUseCase.checkIn(ReservationId.of(id))));
    }

    @PatchMapping("/{id}/checkout")
    public ResponseEntity<ApiResponse<ReservationResponse>> checkOut(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(
                reservationUseCase.checkOut(ReservationId.of(id))));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<ReservationResponse>> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(
                reservationUseCase.cancel(ReservationId.of(id))));
    }
}
