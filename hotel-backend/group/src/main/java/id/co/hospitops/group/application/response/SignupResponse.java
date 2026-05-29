package id.co.hospitops.group.application.response;

import id.co.hospitops.group.domain.model.Group;
import id.co.hospitops.group.domain.model.GroupAdmin;

import java.util.UUID;

public record SignupResponse(
        UUID groupId,
        String groupName,
        UUID adminId,
        String adminEmail
) {
    public static SignupResponse from(Group group, GroupAdmin admin) {
        return new SignupResponse(
                group.getId().value(),
                group.getName(),
                admin.getId().value(),
                admin.getEmail());
    }
}
