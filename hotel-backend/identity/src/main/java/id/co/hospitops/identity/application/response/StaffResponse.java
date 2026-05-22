package id.co.hospitops.identity.application.response;

import id.co.hospitops.identity.domain.model.Staff;
import id.co.hospitops.identity.domain.model.StaffRole;
import id.co.hospitops.shared.StaffId;

import java.time.LocalDateTime;

public record StaffResponse(
        StaffId id,
        String fullName,
        String username,
        StaffRole role,
        boolean active,
        LocalDateTime createdAt
) {
    public static StaffResponse from(Staff staff) {
        return new StaffResponse(
                staff.getId(), staff.getFullName(), staff.getUsername(),
                staff.getRole(), staff.isActive(), staff.getCreatedAt()
        );
    }
}
