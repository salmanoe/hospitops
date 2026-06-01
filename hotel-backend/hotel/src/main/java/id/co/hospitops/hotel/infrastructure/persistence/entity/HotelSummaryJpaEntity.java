package id.co.hospitops.hotel.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ConcreteProxy;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@ConcreteProxy
@Table(name = "hotel_summary")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotelSummaryJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID hotelId;

    @Column(nullable = false, length = 200)
    private String hotelName;

    @Column(nullable = false, length = 20)
    private String hotelStatus;

    @Column(nullable = false)
    private int occupiedRooms;

    @Column(nullable = false)
    private int totalRooms;

    @Column(nullable = false)
    private int arrivalsToday;

    @Column(nullable = false)
    private int departuresToday;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal revenueToday;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal revenueMonth;

    @Column(nullable = false)
    private int dirtyRooms;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
