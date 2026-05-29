package id.co.hospitops.group.domain;

import id.co.hospitops.group.domain.model.Group;
import id.co.hospitops.group.domain.model.GroupAdmin;
import id.co.hospitops.shared.GroupAdminId;
import id.co.hospitops.shared.GroupId;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Group domain")
class GroupDomainTest {

    @Nested
    @DisplayName("Group.create()")
    class GroupCreate {

        @Test
        @DisplayName("assigns a generated ID")
        void assignsId() {
            Group g = Group.create("Acme Hotels", "admin@acme.com");
            assertThat(g.getId()).isNotNull();
        }

        @Test
        @DisplayName("stores name and ownerEmail")
        void storesFields() {
            Group g = Group.create("Acme Hotels", "admin@acme.com");
            assertThat(g.getName()).isEqualTo("Acme Hotels");
            assertThat(g.getOwnerEmail()).isEqualTo("admin@acme.com");
        }

        @Test
        @DisplayName("rejects blank name")
        void rejectsBlankName() {
            assertThatThrownBy(() -> Group.create("  ", "admin@acme.com"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejects blank owner email")
        void rejectsBlankEmail() {
            assertThatThrownBy(() -> Group.create("Acme", ""))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Group.rename()")
    class GroupRename {

        @Test
        @DisplayName("updates the name")
        void updatesName() {
            Group g = Group.create("Acme Hotels", "admin@acme.com");
            g.rename("Acme International");
            assertThat(g.getName()).isEqualTo("Acme International");
        }

        @Test
        @DisplayName("rejects blank name")
        void rejectsBlankName() {
            Group g = Group.create("Acme Hotels", "admin@acme.com");
            assertThatThrownBy(() -> g.rename(""))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("GroupAdmin.create()")
    class GroupAdminCreate {

        @Test
        @DisplayName("assigns a generated ID")
        void assignsId() {
            GroupAdmin a = GroupAdmin.create(GroupId.generate(), "admin@acme.com", "$2a$hash");
            assertThat(a.getId()).isNotNull();
        }

        @Test
        @DisplayName("stores groupId, email, and passwordHash")
        void storesFields() {
            GroupId groupId = GroupId.generate();
            GroupAdmin a = GroupAdmin.create(groupId, "admin@acme.com", "$2a$hash");
            assertThat(a.getGroupId()).isEqualTo(groupId);
            assertThat(a.getEmail()).isEqualTo("admin@acme.com");
            assertThat(a.getPasswordHash()).isEqualTo("$2a$hash");
        }

        @Test
        @DisplayName("rejects null groupId")
        void rejectsNullGroupId() {
            assertThatThrownBy(() -> GroupAdmin.create(null, "admin@acme.com", "hash"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejects blank email")
        void rejectsBlankEmail() {
            assertThatThrownBy(() -> GroupAdmin.create(GroupId.generate(), "  ", "hash"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejects blank password hash")
        void rejectsBlankPasswordHash() {
            assertThatThrownBy(() -> GroupAdmin.create(GroupId.generate(), "e@e.com", ""))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("GroupAdmin.changePassword()")
    class ChangePassword {

        @Test
        @DisplayName("updates the password hash")
        void updatesHash() {
            GroupAdmin a = GroupAdmin.create(GroupId.generate(), "e@e.com", "old-hash");
            a.changePassword("new-hash");
            assertThat(a.getPasswordHash()).isEqualTo("new-hash");
        }

        @Test
        @DisplayName("rejects blank hash")
        void rejectsBlankHash() {
            GroupAdmin a = GroupAdmin.create(GroupId.generate(), "e@e.com", "old-hash");
            assertThatThrownBy(() -> a.changePassword(""))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Group.reconstitute()")
    class Reconstitute {

        @Test
        @DisplayName("preserves all fields including timestamps")
        void preservesAllFields() {
            GroupId id = GroupId.generate();
            LocalDateTime created = LocalDateTime.of(2026, 1, 1, 0, 0);
            LocalDateTime updated = LocalDateTime.of(2026, 6, 1, 0, 0);

            Group g = Group.reconstitute(id, "Acme", "e@e.com", created, updated);

            assertThat(g.getId()).isEqualTo(id);
            assertThat(g.getName()).isEqualTo("Acme");
            assertThat(g.getOwnerEmail()).isEqualTo("e@e.com");
            assertThat(g.getCreatedAt()).isEqualTo(created);
            assertThat(g.getUpdatedAt()).isEqualTo(updated);
        }
    }
}
