package id.co.hospitops.group.domain.model;

import id.co.hospitops.shared.GroupAdminId;
import id.co.hospitops.shared.GroupId;
import id.co.hospitops.shared.Guard;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * A GROUP_ADMIN account that belongs to a {@link Group}.
 * Credentials are stored as a BCrypt hash — never plain text.
 */
@Getter
public class GroupAdmin {

    private final GroupAdminId id;
    private final GroupId groupId;
    private final String email;
    private String passwordHash;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static GroupAdmin create(GroupId groupId, String email, String passwordHash) {
        Guard.notNull(groupId, "GroupId");
        Guard.notBlank(email, "Email");
        Guard.notBlank(passwordHash, "Password hash");
        LocalDateTime now = LocalDateTime.now();
        return new GroupAdmin(GroupAdminId.generate(), groupId, email, passwordHash, now, now);
    }

    public static GroupAdmin reconstitute(GroupAdminId id, GroupId groupId, String email,
                                          String passwordHash, LocalDateTime createdAt,
                                          LocalDateTime updatedAt) {
        return new GroupAdmin(id, groupId, email, passwordHash, createdAt, updatedAt);
    }

    private GroupAdmin(GroupAdminId id, GroupId groupId, String email,
                       String passwordHash, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.groupId = groupId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void changePassword(String newPasswordHash) {
        Guard.notBlank(newPasswordHash, "Password hash");
        this.passwordHash = newPasswordHash;
        this.updatedAt = LocalDateTime.now();
    }
}
