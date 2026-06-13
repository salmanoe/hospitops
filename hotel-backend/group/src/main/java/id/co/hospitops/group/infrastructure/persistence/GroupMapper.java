package id.co.hospitops.group.infrastructure.persistence;

import id.co.hospitops.group.domain.model.Group;
import id.co.hospitops.group.infrastructure.persistence.entity.GroupJpaEntity;
import id.co.hospitops.shared.GroupId;
import org.springframework.stereotype.Component;

@Component
class GroupMapper {

    GroupJpaEntity toJpa(Group g) {
        return GroupJpaEntity.builder()
                .id(g.getId().value())
                .name(g.getName())
                .ownerEmail(g.getOwnerEmail())
                .build();
    }

    Group toDomain(GroupJpaEntity e) {
        return Group.reconstitute(
                GroupId.of(e.getId()),
                e.getName(),
                e.getOwnerEmail(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }
}
