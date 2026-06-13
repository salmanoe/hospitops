package id.co.hospitops.channel.infrastructure.persistence.entity;

import id.co.hospitops.channel.domain.model.InboundStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ConcreteProxy;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@ConcreteProxy
@Table(name = "channel_inbound_booking",
        indexes = {
                @Index(name = "idx_channel_inbound_hotel_id", columnList = "hotel_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_channel_inbound_hotel_booking",
                        columnNames = {"hotel_id", "external_booking_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChannelInboundBookingJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "hotel_id", nullable = false, columnDefinition = "uuid")
    private UUID hotelId;

    @Column(name = "external_booking_id", nullable = false, length = 128)
    private String externalBookingId;

    @Column(name = "reservation_id", columnDefinition = "uuid")
    private UUID reservationId;

    @Column(name = "last_revision_id", length = 128)
    private String lastRevisionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private InboundStatus status;

    @Column(name = "ota_name", length = 64)
    private String otaName;

    @Column(name = "ota_reservation_code", length = 128)
    private String otaReservationCode;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
