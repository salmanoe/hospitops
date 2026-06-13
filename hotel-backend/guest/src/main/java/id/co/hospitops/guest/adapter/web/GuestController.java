package id.co.hospitops.guest.adapter.web;

import id.co.hospitops.guest.adapter.web.request.*;
import id.co.hospitops.guest.application.command.*;
import id.co.hospitops.guest.application.response.*;
import id.co.hospitops.guest.domain.port.in.ManageGuestUseCase;
import id.co.hospitops.shared.GuestId;
import id.co.hospitops.shared.web.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequiresHotelContext
@RestController
@RequestMapping("/api/v1/guests")
@RequiredArgsConstructor
public class GuestController {

    private final ManageGuestUseCase guestUseCase;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResult<GuestResponse>>> search(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("fullName"));
        return ResponseEntity.ok(ApiResponse.ok(guestUseCase.search(q, pageable)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<GuestSearchResult>>> quickSearch(
            @RequestParam String q) {
        return ResponseEntity.ok(ApiResponse.ok(guestUseCase.quickSearch(q)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GuestResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(guestUseCase.findById(GuestId.of(id))));
    }

    @PostMapping
    @SuppressWarnings("JvmTaintAnalysis")
    // @Valid enforces Bean Validation on all string inputs; response is JSON, not HTML
    public ResponseEntity<ApiResponse<GuestResponse>> register(
            @Valid @RequestBody RegisterGuestRequest req) {
        var cmd = new RegisterGuestCommand(req.fullName(), req.idNumber(),
                req.nationality(), req.phone(), req.email(), req.address());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(guestUseCase.register(cmd)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<GuestResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateGuestRequest req) {
        var cmd = new UpdateGuestCommand(req.fullName(), req.nationality(),
                req.phone(), req.email(), req.address());
        return ResponseEntity.ok(ApiResponse.ok(guestUseCase.update(GuestId.of(id), cmd)));
    }
}
