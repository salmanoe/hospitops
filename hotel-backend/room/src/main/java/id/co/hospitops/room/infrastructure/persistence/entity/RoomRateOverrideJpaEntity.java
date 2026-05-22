package id.co.hospitops.room.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ConcreteProxy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@ConcreteProxy
@Table(name = "room_rate_override", indexes = {
        @Index(name = "idx_rro_room_type_id", columnList = "room_type_id"),
        @Index(name = "idx_rro_valid_range",  columnList = "room_type_id,valid_from,valid_until")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomRateOverrideJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "room_type_id", nullable = false, columnDefinition = "uuid")
    private UUID roomTypeId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "price_override", nullable = false, precision = 15, scale = 2)
    private BigDecimal priceOverride;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_until", nullable = false)
    private LocalDate validUntil;
}
