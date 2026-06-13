package id.co.hospitops.group.application;

import id.co.hospitops.group.application.command.SignupGroupCommand;
import id.co.hospitops.group.application.response.SignupResponse;
import id.co.hospitops.group.domain.model.Group;
import id.co.hospitops.group.domain.model.GroupAdmin;
import id.co.hospitops.group.domain.port.out.GroupAdminRepository;
import id.co.hospitops.group.domain.port.out.GroupRepository;
import id.co.hospitops.shared.GroupId;
import id.co.hospitops.shared.exception.ConflictException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("GroupService")
@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock GroupRepository groupRepo;
    @Mock GroupAdminRepository adminRepo;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks GroupService service;

    @Nested
    @DisplayName("signup()")
    class Signup {

        @Test
        @DisplayName("creates group and admin, returns signup response")
        void createsGroupAndAdmin() {
            var cmd = new SignupGroupCommand("Acme Hotels", "admin@acme.com", "secret123");
            Group savedGroup = Group.reconstitute(
                    GroupId.generate(), "Acme Hotels", "admin@acme.com",
                    LocalDateTime.now(), LocalDateTime.now());
            GroupAdmin savedAdmin = GroupAdmin.create(
                    savedGroup.getId(), "admin@acme.com", "$2a$hash");

            when(adminRepo.existsByEmail("admin@acme.com")).thenReturn(false);
            when(passwordEncoder.encode("secret123")).thenReturn("$2a$hash");
            when(groupRepo.save(any())).thenReturn(savedGroup);
            when(adminRepo.save(any())).thenReturn(savedAdmin);

            SignupResponse response = service.signup(cmd);

            assertThat(response.groupId()).isEqualTo(savedGroup.getId().value());
            assertThat(response.groupName()).isEqualTo("Acme Hotels");
            assertThat(response.adminEmail()).isEqualTo("admin@acme.com");
        }

        @Test
        @DisplayName("encodes the raw password before saving")
        void encodesPassword() {
            var cmd = new SignupGroupCommand("Acme", "a@a.com", "plaintext");
            Group savedGroup = Group.reconstitute(
                    GroupId.generate(), "Acme", "a@a.com",
                    LocalDateTime.now(), LocalDateTime.now());

            when(adminRepo.existsByEmail("a@a.com")).thenReturn(false);
            when(passwordEncoder.encode("plaintext")).thenReturn("hashed");
            when(groupRepo.save(any())).thenReturn(savedGroup);
            when(adminRepo.save(any(GroupAdmin.class))).thenAnswer(inv -> inv.getArgument(0));

            service.signup(cmd);

            var captor = ArgumentCaptor.forClass(GroupAdmin.class);
            verify(adminRepo).save(captor.capture());
            assertThat(captor.getValue().getPasswordHash()).isEqualTo("hashed");
        }

        @Test
        @DisplayName("throws ConflictException when email is already registered")
        void throwsOnDuplicateEmail() {
            var cmd = new SignupGroupCommand("Acme", "taken@acme.com", "secret");
            when(adminRepo.existsByEmail("taken@acme.com")).thenReturn(true);

            assertThatThrownBy(() -> service.signup(cmd))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("taken@acme.com");

            verify(groupRepo, never()).save(any());
            verify(adminRepo, never()).save(any());
        }
    }
}
