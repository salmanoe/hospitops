package id.co.hospitops.guest.domain.model;

import id.co.hospitops.shared.GuestId;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class Guest {

    private final GuestId id;
    private String fullName;
    private final String idNumber;
    private String nationality;
    private String phone;
    private String email;
    private String address;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static Guest create(String fullName, String idNumber,
                               String nationality, String phone,
                               String email, String address) {
        validateFullName(fullName);
        return new Guest(GuestId.generate(), fullName, idNumber, nationality,
                phone, email, address,
                LocalDateTime.now(), LocalDateTime.now());
    }

    public static Guest reconstitute(GuestId id, String fullName, String idNumber,
                                     String nationality, String phone, String email,
                                     String address, LocalDateTime createdAt,
                                     LocalDateTime updatedAt) {
        return new Guest(id, fullName, idNumber, nationality,
                phone, email, address, createdAt, updatedAt);
    }

    private Guest(GuestId id, String fullName, String idNumber, String nationality,
                  String phone, String email, String address,
                  LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.fullName = fullName;
        this.idNumber = idNumber;
        this.nationality = nationality;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void updateProfile(String fullName, String nationality,
                              String phone, String email, String address) {
        validateFullName(fullName);
        this.fullName = fullName;
        this.nationality = nationality;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.updatedAt = LocalDateTime.now();
    }

    private static void validateFullName(String name) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Full name cannot be blank");
        if (name.length() > 200)
            throw new IllegalArgumentException("Full name too long (max 200 chars)");
    }
}
