package id.co.hospitops.channel.infrastructure.persistence.entity;

import id.co.hospitops.channel.domain.model.ChannelProvider;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ConcreteProxy;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@ConcreteProxy
@Table(name = "channel_property_mapping",
        indexes = {
                @Index(name = "idx_channel_property_hotel_id", columnList = "hotel_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_channel_property_hotel_provider",
                        columnNames = {"hotel_id", "provider"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChannelPropertyMappingJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Version
    private Long version;

    @Column(name = "hotel_id", nullable = false, columnDefinition = "uuid")
    private UUID hotelId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ChannelProvider provider;

    @Column(name = "external_property_id", nullable = false, length = 128)
    private String externalPropertyId;

    @Column(nullable = false)
    private boolean enabled;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
