package id.co.hospitops.identity.domain.model;

import id.co.hospitops.shared.HotelId;
import id.co.hospitops.shared.StaffId;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class Staff {

    private final StaffId id;
    private String fullName;
    private final String username;
    private String passwordHash;
    private StaffRole role;
    private boolean active;
    /**
     * The hotel this staff member belongs to. Used for data-isolation filtering.
     */
    private final HotelId hotelId;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static Staff create(HotelId hotelId, String fullName, String username,
                               String passwordHash, StaffRole role) {
        return new Staff(
                StaffId.generate(), fullName, username,
                passwordHash, role, true, hotelId,
                LocalDateTime.now(), LocalDateTime.now()
        );
    }

    public static Staff reconstitute(StaffId id, String fullName, String username,
                                     String passwordHash, StaffRole role, boolean active,
                                     HotelId hotelId, LocalDateTime createdAt,
                                     LocalDateTime updatedAt) {
        return new Staff(id, fullName, username, passwordHash, role,
                active, hotelId, createdAt, updatedAt);
    }

    private Staff(StaffId id, String fullName, String username,
                  String passwordHash, StaffRole role, boolean active,
                  HotelId hotelId, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.fullName = fullName;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = active;
        this.hotelId = hotelId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void updateProfile(String fullName) {
        if (fullName == null || fullName.isBlank())
            throw new IllegalArgumentException("Full name cannot be blank");
        this.fullName = fullName;
        this.updatedAt = LocalDateTime.now();
    }

    public void changePassword(String newPasswordHash) {
        if (newPasswordHash == null || newPasswordHash.isBlank())
            throw new IllegalArgumentException("Password hash cannot be blank");
        this.passwordHash = newPasswordHash;
        this.updatedAt = LocalDateTime.now();
    }

    public void changeRole(StaffRole newRole) {
        this.role = newRole;
        this.updatedAt = LocalDateTime.now();
    }

    public void activate() {
        this.active = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean hasRole(StaffRole required) {
        return this.role == required;
    }

    public boolean canAccess(StaffRole... allowedRoles) {
        for (StaffRole r : allowedRoles) {
            if (this.role == r) return true;
        }
        return false;
    }
}
