package id.co.hospitops.group.infrastructure.persistence;

import id.co.hospitops.group.domain.model.GroupAdmin;
import id.co.hospitops.group.infrastructure.persistence.entity.GroupAdminJpaEntity;
import id.co.hospitops.shared.GroupAdminId;
import id.co.hospitops.shared.GroupId;
import org.springframework.stereotype.Component;

@Component
class GroupAdminMapper {

    GroupAdminJpaEntity toJpa(GroupAdmin a) {
        return GroupAdminJpaEntity.builder()
                .id(a.getId().value())
                .groupId(a.getGroupId().value())
                .email(a.getEmail())
                .passwordHash(a.getPasswordHash())
                .build();
    }

    GroupAdmin toDomain(GroupAdminJpaEntity e) {
        return GroupAdmin.reconstitute(
                GroupAdminId.of(e.getId()),
                GroupId.of(e.getGroupId()),
                e.getEmail(),
                e.getPasswordHash(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }
}
