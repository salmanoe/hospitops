package id.co.hospitops.hotel.infrastructure.persistence;

import id.co.hospitops.hotel.infrastructure.persistence.entity.SetupChecklistJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface SetupChecklistJpaRepository extends JpaRepository<SetupChecklistJpaEntity, UUID> {
}
