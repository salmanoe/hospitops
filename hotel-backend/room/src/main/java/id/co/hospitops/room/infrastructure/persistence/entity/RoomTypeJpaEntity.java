package id.co.hospitops.room.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ConcreteProxy;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@ConcreteProxy
@Table(name = "room_type",
        indexes = {
                @Index(name = "idx_room_type_name", columnList = "name"),
                @Index(name = "idx_room_type_hotel_id", columnList = "hotel_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_room_type_name_hotel", columnNames = {"name", "hotel_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomTypeJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Version
    private Long version;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private int capacity;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "base_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal basePrice;

    @Column(nullable = false, columnDefinition = "uuid")
    private UUID hotelId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
