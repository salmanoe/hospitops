package id.co.hospitops.group.domain.port.out;

import id.co.hospitops.group.domain.model.Group;
import id.co.hospitops.shared.GroupId;

import java.util.Optional;

public interface GroupRepository {
    Group save(Group group);
    Optional<Group> findById(GroupId id);
    boolean existsByOwnerEmail(String email);
}
