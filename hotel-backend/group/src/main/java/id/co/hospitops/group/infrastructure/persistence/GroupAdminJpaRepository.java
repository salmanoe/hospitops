package id.co.hospitops.group.infrastructure.persistence;

import id.co.hospitops.group.infrastructure.persistence.entity.GroupAdminJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface GroupAdminJpaRepository extends JpaRepository<GroupAdminJpaEntity, UUID> {
    Optional<GroupAdminJpaEntity> findByEmail(String email);
    boolean existsByEmail(String email);
    List<GroupAdminJpaEntity> findByGroupId(UUID groupId);
}
