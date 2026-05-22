package id.co.hospitops.reservation.infrastructure.persistence.entity;

import id.co.hospitops.reservation.domain.model.ReservationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ConcreteProxy;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@ConcreteProxy
@Table(name = "reservation", indexes = {
        @Index(name = "idx_reservation_number", columnList = "reservation_number"),
        @Index(name = "idx_reservation_guest_id", columnList = "guest_id"),
        @Index(name = "idx_reservation_room_id", columnList = "room_id"),
        @Index(name = "idx_reservation_status", columnList = "status"),
        @Index(name = "idx_reservation_check_in", columnList = "check_in_date"),
        @Index(name = "idx_reservation_check_out", columnList = "check_out_date"),
        @Index(name = "idx_reservation_availability", columnList = "room_id,check_in_date,check_out_date,status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, unique = true, length = 20)
    private String reservationNumber;

    @Column(name = "guest_id", nullable = false, columnDefinition = "uuid")
    private UUID guestId;

    @Column(name = "room_id", nullable = false, columnDefinition = "uuid")
    private UUID roomId;

    @Column(name = "created_by", columnDefinition = "uuid")
    private UUID createdBy;

    @Column(nullable = false)
    private LocalDate checkInDate;

    @Column(nullable = false)
    private LocalDate checkOutDate;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false)
    private ReservationStatus status;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal ratePerNight;

    @Column(nullable = false)
    private int adults;

    @Column(nullable = false)
    private int children;

    @Column(columnDefinition = "text")
    private String specialRequests;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
