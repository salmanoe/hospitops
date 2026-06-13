package id.co.hospitops.group.application.response;

import id.co.hospitops.group.domain.model.Group;

import java.time.LocalDateTime;
import java.util.UUID;

public record GroupResponse(
        UUID id,
        String name,
        String ownerEmail,
        LocalDateTime createdAt
) {
    public static GroupResponse from(Group g) {
        return new GroupResponse(g.getId().value(), g.getName(),
                g.getOwnerEmail(), g.getCreatedAt());
    }
}
