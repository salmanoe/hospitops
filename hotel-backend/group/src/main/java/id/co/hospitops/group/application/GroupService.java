package id.co.hospitops.group.application;

import id.co.hospitops.group.application.command.SignupGroupCommand;
import id.co.hospitops.group.application.response.GroupResponse;
import id.co.hospitops.group.application.response.SignupResponse;
import id.co.hospitops.group.domain.model.Group;
import id.co.hospitops.group.domain.model.GroupAdmin;
import id.co.hospitops.group.domain.port.in.ManageGroupUseCase;
import id.co.hospitops.group.domain.port.out.GroupAdminRepository;
import id.co.hospitops.group.domain.port.out.GroupRepository;
import id.co.hospitops.shared.GroupId;
import id.co.hospitops.shared.exception.ConflictException;
import id.co.hospitops.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class GroupService implements ManageGroupUseCase {

    private final GroupRepository groupRepo;
    private final GroupAdminRepository adminRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    public SignupResponse signup(SignupGroupCommand cmd) {
        if (adminRepo.existsByEmail(cmd.adminEmail())) {
            throw new ConflictException(
                    "An account already exists for email: " + cmd.adminEmail());
        }

        Group group = Group.create(cmd.groupName(), cmd.adminEmail());
        Group saved = groupRepo.save(group);

        String hash = passwordEncoder.encode(cmd.rawPassword());
        GroupAdmin admin = GroupAdmin.create(saved.getId(), cmd.adminEmail(), hash);
        GroupAdmin savedAdmin = adminRepo.save(admin);

        return SignupResponse.from(saved, savedAdmin);
    }

    @Override
    @Transactional(readOnly = true)
    public GroupResponse findById(GroupId id) {
        Group group = groupRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Group", id.value()));
        return GroupResponse.from(group);
    }
}
