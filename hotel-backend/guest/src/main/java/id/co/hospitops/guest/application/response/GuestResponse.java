package id.co.hospitops.guest.application.response;

import id.co.hospitops.guest.domain.model.Guest;
import id.co.hospitops.shared.GuestId;

import java.time.LocalDateTime;

public record GuestResponse(
        GuestId id,
        String fullName,
        String idNumber,
        String nationality,
        String phone,
        String email,
        String address,
        LocalDateTime createdAt
) {
    public static GuestResponse from(Guest g) {
        return new GuestResponse(g.getId(), g.getFullName(), g.getIdNumber(),
                g.getNationality(), g.getPhone(), g.getEmail(),
                g.getAddress(), g.getCreatedAt());
    }
}
