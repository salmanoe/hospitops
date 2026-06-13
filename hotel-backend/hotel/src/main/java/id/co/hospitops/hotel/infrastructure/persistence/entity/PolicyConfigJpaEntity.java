package id.co.hospitops.hotel.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ConcreteProxy;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@ConcreteProxy
@Table(name = "hotel_policy_config",
        indexes = @Index(name = "idx_policy_config_hotel_id", columnList = "hotel_id", unique = true))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyConfigJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "hotel_id", nullable = false, columnDefinition = "uuid")
    private UUID hotelId;

    @Column(nullable = false)
    private int taxPercent;

    @Column(nullable = false, length = 50)
    private String taxName;

    @Column(nullable = false, length = 200)
    private String invoiceHotelName;

    @Column(columnDefinition = "text")
    private String invoiceAddress;

    @Column(columnDefinition = "text")
    private String invoiceFooterNote;

    @Version
    private Long version;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
