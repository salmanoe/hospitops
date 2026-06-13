package id.co.hospitops.group.domain.port.out;

import id.co.hospitops.group.domain.model.GroupAdmin;
import id.co.hospitops.shared.GroupAdminId;
import id.co.hospitops.shared.GroupId;

import java.util.Optional;

public interface GroupAdminRepository {
    GroupAdmin save(GroupAdmin admin);
    Optional<GroupAdmin> findById(GroupAdminId id);
    Optional<GroupAdmin> findByEmail(String email);
    boolean existsByEmail(String email);
    java.util.List<GroupAdmin> findByGroupId(GroupId groupId);
}
