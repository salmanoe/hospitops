package id.co.hospitops.channel.infrastructure.persistence.entity;

import id.co.hospitops.channel.domain.model.SyncMessageType;
import id.co.hospitops.channel.domain.model.SyncStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ConcreteProxy;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@ConcreteProxy
@Table(name = "channel_sync_message",
        indexes = {
                @Index(name = "idx_channel_sync_due", columnList = "status, next_attempt_at"),
                @Index(name = "idx_channel_sync_hotel_id", columnList = "hotel_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChannelSyncMessageJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "hotel_id", nullable = false, columnDefinition = "uuid")
    private UUID hotelId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SyncMessageType type;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SyncStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
