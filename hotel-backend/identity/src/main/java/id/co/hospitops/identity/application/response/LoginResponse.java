package id.co.hospitops.identity.application.response;

import id.co.hospitops.identity.domain.model.Staff;
import id.co.hospitops.identity.domain.model.StaffRole;
import id.co.hospitops.shared.StaffId;

public record LoginResponse(
        String token,
        String tokenType,
        long expiresIn,
        String refreshToken,
        long refreshExpiresIn,
        StaffId staffId,
        String fullName,
        String username,
        StaffRole role
) {
    public static LoginResponse of(String token, long expiresIn,
                                   String refreshToken, long refreshExpiresIn,
                                   Staff staff) {
        return new LoginResponse(
                token, "Bearer", expiresIn,
                refreshToken, refreshExpiresIn,
                staff.getId(), staff.getFullName(),
                staff.getUsername(), staff.getRole()
        );
    }
}
