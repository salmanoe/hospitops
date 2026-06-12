package id.co.hospitops.channel.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ConcreteProxy;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@ConcreteProxy
@Table(name = "channel_room_type_mapping",
        indexes = {
                @Index(name = "idx_channel_rt_hotel_id", columnList = "hotel_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_channel_rt_hotel_room_type",
                        columnNames = {"hotel_id", "room_type_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChannelRoomTypeMappingJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Version
    private Long version;

    @Column(name = "hotel_id", nullable = false, columnDefinition = "uuid")
    private UUID hotelId;

    @Column(name = "room_type_id", nullable = false, columnDefinition = "uuid")
    private UUID roomTypeId;

    @Column(name = "external_room_type_id", nullable = false, length = 128)
    private String externalRoomTypeId;

    @Column(name = "external_rate_plan_id", nullable = false, length = 128)
    private String externalRatePlanId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
