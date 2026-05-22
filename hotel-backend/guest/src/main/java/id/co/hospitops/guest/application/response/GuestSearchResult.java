package id.co.hospitops.guest.application.response;

import id.co.hospitops.guest.domain.model.Guest;
import id.co.hospitops.shared.GuestId;

public record GuestSearchResult(
        GuestId id,
        String fullName,
        String idNumber,
        String phone
) {
    public static GuestSearchResult from(Guest g) {
        return new GuestSearchResult(g.getId(), g.getFullName(),
                g.getIdNumber(), g.getPhone());
    }
}
