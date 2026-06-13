package id.co.hospitops.group.domain.model;

import id.co.hospitops.shared.GroupId;
import id.co.hospitops.shared.Guard;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Aggregate root representing a hotel group (chain / management company).
 * A group owns one or more hotels and has at least one GROUP_ADMIN account.
 */
@Getter
public class Group {

    private final GroupId id;
    private String name;
    private final String ownerEmail;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static Group create(String name, String ownerEmail) {
        Guard.notBlank(name, "Group name");
        Guard.notBlank(ownerEmail, "Owner email");
        LocalDateTime now = LocalDateTime.now();
        return new Group(GroupId.generate(), name, ownerEmail, now, now);
    }

    public static Group reconstitute(GroupId id, String name, String ownerEmail,
                                     LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Group(id, name, ownerEmail, createdAt, updatedAt);
    }

    private Group(GroupId id, String name, String ownerEmail,
                  LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.ownerEmail = ownerEmail;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void rename(String newName) {
        Guard.notBlank(newName, "Group name");
        this.name = newName;
        this.updatedAt = LocalDateTime.now();
    }
}
