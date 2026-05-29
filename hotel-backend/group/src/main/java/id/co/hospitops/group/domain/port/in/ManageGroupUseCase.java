package id.co.hospitops.group.domain.port.in;

import id.co.hospitops.group.application.command.SignupGroupCommand;
import id.co.hospitops.group.application.response.GroupResponse;
import id.co.hospitops.group.application.response.SignupResponse;
import id.co.hospitops.shared.GroupId;

public interface ManageGroupUseCase {

    /** Creates a new group and its first GROUP_ADMIN account. */
    SignupResponse signup(SignupGroupCommand cmd);

    /** Returns the group profile. */
    GroupResponse findById(GroupId id);
}
