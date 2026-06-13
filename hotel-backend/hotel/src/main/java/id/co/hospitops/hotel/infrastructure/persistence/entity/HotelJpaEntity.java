package id.co.hospitops.hotel.infrastructure.persistence.entity;

import id.co.hospitops.hotel.domain.model.HotelStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ConcreteProxy;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@ConcreteProxy
@Table(name = "hotel",
        indexes = {
                @Index(name = "idx_hotel_group_id", columnList = "group_id"),
                @Index(name = "idx_hotel_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotelJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, columnDefinition = "uuid")
    private UUID groupId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "text")
    private String address;

    @Column(nullable = false, length = 50)
    private String timezone;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private int starRating;

    @Column(nullable = false)
    private LocalTime defaultCheckInTime;

    @Column(nullable = false)
    private LocalTime defaultCheckOutTime;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private HotelStatus status;

    @Version
    private Long version;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
