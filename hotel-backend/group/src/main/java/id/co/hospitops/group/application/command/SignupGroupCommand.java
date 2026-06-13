package id.co.hospitops.group.application.command;

public record SignupGroupCommand(
        String groupName,
        String adminEmail,
        String rawPassword
) {}
