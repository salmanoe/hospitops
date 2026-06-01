package id.co.hospitops.room.infrastructure.persistence.entity;

import id.co.hospitops.room.domain.model.RoomStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ConcreteProxy;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@ConcreteProxy
@Table(name = "room",
        indexes = {
                @Index(name = "idx_room_number", columnList = "room_number"),
                @Index(name = "idx_room_status", columnList = "status"),
                @Index(name = "idx_room_type_id", columnList = "room_type_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_room_number_hotel", columnNames = {"room_number", "hotel_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Version
    private Long version;

    @Column(name = "room_number", nullable = false, length = 10)
    private String roomNumber;

    @Column(nullable = false)
    private int floor;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false)
    private RoomStatus status;

    @Column(name = "room_type_id", nullable = false, columnDefinition = "uuid")
    private UUID roomTypeId;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(nullable = false, columnDefinition = "uuid")
    private UUID hotelId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
