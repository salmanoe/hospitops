package id.co.hospitops.identity.infrastructure.persistence;

import id.co.hospitops.identity.domain.model.Staff;
import id.co.hospitops.identity.infrastructure.persistence.entity.StaffJpaEntity;
import id.co.hospitops.shared.StaffId;
import org.springframework.stereotype.Component;

@Component
class StaffMapper {

    StaffJpaEntity toJpa(Staff staff) {
        return StaffJpaEntity.builder()
                .id(staff.getId().value())
                .fullName(staff.getFullName())
                .username(staff.getUsername())
                .passwordHash(staff.getPasswordHash())
                .role(staff.getRole())
                .active(staff.isActive())
                .createdAt(staff.getCreatedAt())
                .updatedAt(staff.getUpdatedAt())
                .build();
    }

    Staff toDomain(StaffJpaEntity e) {
        return Staff.reconstitute(
                StaffId.of(e.getId()), e.getFullName(), e.getUsername(),
                e.getPasswordHash(), e.getRole(), e.isActive(),
                e.getCreatedAt(), e.getUpdatedAt()
        );
    }
}
