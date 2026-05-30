package id.co.hospitops.housekeeping.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@ConcreteProxy
@Table(name = "housekeeping_task", indexes = {
        @Index(name = "idx_hk_room", columnList = "room_id"),
        @Index(name = "idx_hk_completed", columnList = "completed")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HousekeepingTaskJpaEntity {
    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;
    @Column(name = "hotel_id", nullable = false, columnDefinition = "uuid")
    private UUID hotelId;
    @Column(name = "room_id", nullable = false, columnDefinition = "uuid")
    private UUID roomId;
    @Column(name = "reservation_id", columnDefinition = "uuid")
    private UUID reservationId;
    @Column(name = "assigned_to", columnDefinition = "uuid")
    private UUID assignedTo;
    @Column(columnDefinition = "text")
    private String notes;
    @Column(nullable = false)
    private boolean completed;
    private LocalDateTime completedAt;
    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
