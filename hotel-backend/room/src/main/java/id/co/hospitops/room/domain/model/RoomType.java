package id.co.hospitops.room.domain.model;

import id.co.hospitops.shared.HotelId;
import id.co.hospitops.shared.Money;
import id.co.hospitops.shared.RoomTypeId;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class RoomType {

    private final RoomTypeId id;
    private String name;
    private int capacity;
    private String description;
    private Money basePrice;
    private final HotelId hotelId;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static RoomType create(HotelId hotelId, String name, int capacity,
                                  String description, Money basePrice) {
        validate(name, capacity, basePrice);
        return new RoomType(RoomTypeId.generate(), name, capacity,
                description, basePrice, hotelId,
                LocalDateTime.now(), LocalDateTime.now());
    }

    public static RoomType reconstitute(RoomTypeId id, String name, int capacity,
                                        String description, Money basePrice,
                                        HotelId hotelId,
                                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new RoomType(id, name, capacity, description,
                basePrice, hotelId, createdAt, updatedAt);
    }

    private RoomType(RoomTypeId id, String name, int capacity, String description,
                     Money basePrice, HotelId hotelId,
                     LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.capacity = capacity;
        this.description = description;
        this.basePrice = basePrice;
        this.hotelId = hotelId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void update(String name, int capacity, String description, Money basePrice) {
        validate(name, capacity, basePrice);
        this.name = name;
        this.capacity = capacity;
        this.description = description;
        this.basePrice = basePrice;
        this.updatedAt = LocalDateTime.now();
    }

    // R-10 FIX: The original condition was:
    //   basePrice.isZero() && !basePrice.amount().equals(BigDecimal.ZERO)
    // which is always false (if isZero() is true then equals(ZERO) must be true).
    // The intent is to reject null or negative prices; zero is a valid price.
    private static void validate(String name, int capacity, Money basePrice) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Room type name cannot be blank");
        if (capacity < 1)
            throw new IllegalArgumentException("Capacity must be at least 1");
        if (basePrice == null)
            throw new IllegalArgumentException("Base price cannot be null");
        if (basePrice.amount().compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Base price cannot be negative");
    }
}
