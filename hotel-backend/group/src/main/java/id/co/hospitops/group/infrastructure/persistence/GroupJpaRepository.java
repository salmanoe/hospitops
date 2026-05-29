package id.co.hospitops.group.infrastructure.persistence;

import id.co.hospitops.group.infrastructure.persistence.entity.GroupJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface GroupJpaRepository extends JpaRepository<GroupJpaEntity, UUID> {
    boolean existsByOwnerEmail(String ownerEmail);
}
