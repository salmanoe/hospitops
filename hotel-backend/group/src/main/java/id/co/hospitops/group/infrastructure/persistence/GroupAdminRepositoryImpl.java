package id.co.hospitops.group.infrastructure.persistence;

import id.co.hospitops.group.domain.model.GroupAdmin;
import id.co.hospitops.group.domain.port.out.GroupAdminRepository;
import id.co.hospitops.shared.GroupAdminId;
import id.co.hospitops.shared.GroupId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
class GroupAdminRepositoryImpl implements GroupAdminRepository {

    private final GroupAdminJpaRepository jpa;
    private final GroupAdminMapper mapper;

    @Override
    public GroupAdmin save(GroupAdmin admin) {
        return mapper.toDomain(jpa.save(mapper.toJpa(admin)));
    }

    @Override
    public Optional<GroupAdmin> findById(GroupAdminId id) {
        return jpa.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<GroupAdmin> findByEmail(String email) {
        return jpa.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpa.existsByEmail(email);
    }

    @Override
    public List<GroupAdmin> findByGroupId(GroupId groupId) {
        return jpa.findByGroupId(groupId.value())
                .stream().map(mapper::toDomain).toList();
    }
}
