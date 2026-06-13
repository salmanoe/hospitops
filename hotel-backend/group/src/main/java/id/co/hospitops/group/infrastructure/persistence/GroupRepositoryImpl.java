package id.co.hospitops.group.infrastructure.persistence;

import id.co.hospitops.group.domain.model.Group;
import id.co.hospitops.group.domain.port.out.GroupRepository;
import id.co.hospitops.shared.GroupId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
class GroupRepositoryImpl implements GroupRepository {

    private final GroupJpaRepository jpa;
    private final GroupMapper mapper;

    @Override
    public Group save(Group group) {
        return mapper.toDomain(jpa.save(mapper.toJpa(group)));
    }

    @Override
    public Optional<Group> findById(GroupId id) {
        return jpa.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public boolean existsByOwnerEmail(String email) {
        return jpa.existsByOwnerEmail(email);
    }
}
