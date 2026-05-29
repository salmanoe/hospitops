package id.co.hospitops.hotel.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ConcreteProxy;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@ConcreteProxy
@Table(name = "hotel_setup_checklist")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SetupChecklistJpaEntity {

    /** hotel_id is both the PK and the FK to hotel — 1:1 relationship. */
    @Id
    @Column(columnDefinition = "uuid")
    private UUID hotelId;

    @Column(nullable = false)
    private boolean profileComplete;

    @Column(nullable = false)
    private boolean policyComplete;

    @Column(nullable = false)
    private boolean roomTypeAdded;

    @Column(nullable = false)
    private boolean roomAdded;

    @Column(nullable = false)
    private boolean staffAccountCreated;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
