package id.co.hospitops.guest.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ConcreteProxy;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@ConcreteProxy
@Table(name = "guest",
        indexes = {
                @Index(name = "idx_guest_full_name", columnList = "full_name"),
                @Index(name = "idx_guest_id_number", columnList = "id_number"),
                @Index(name = "idx_guest_email", columnList = "email")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuestJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, length = 200)
    private String fullName;

    @Column(unique = true, length = 50)
    private String idNumber;

    @Column(length = 100)
    private String nationality;

    @Column(length = 30)
    private String phone;

    @Column(length = 150)
    private String email;

    @Column(columnDefinition = "text")
    private String address;

    @Column(nullable = false, columnDefinition = "uuid")
    private UUID hotelId;

    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
