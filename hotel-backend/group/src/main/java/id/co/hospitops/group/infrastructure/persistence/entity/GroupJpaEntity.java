package id.co.hospitops.group.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ConcreteProxy;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@ConcreteProxy
@Table(name = "\"group\"",
        indexes = @Index(name = "idx_group_owner_email", columnList = "owner_email"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 150, unique = true)
    private String ownerEmail;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
