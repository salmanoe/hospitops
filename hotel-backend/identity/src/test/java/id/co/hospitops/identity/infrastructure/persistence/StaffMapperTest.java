package id.co.hospitops.identity.infrastructure.persistence;

import id.co.hospitops.identity.domain.model.Staff;
import id.co.hospitops.identity.domain.model.StaffRole;
import id.co.hospitops.identity.infrastructure.persistence.entity.StaffJpaEntity;
import id.co.hospitops.shared.StaffId;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for StaffMapper — hand-written @Component (not MapStruct).
 * Instantiated directly; no Spring context needed.
 *
 * Covers:
 * - toJpa()  : all fields, active/inactive, every role
 * - toDomain(): all fields, preserves timestamps
 * - Round-trip: Staff -> toJpa() -> toDomain() identity check
 */
@DisplayName("StaffMapper")
class StaffMapperTest {

    private final StaffMapper mapper = new StaffMapper();

    // ── toJpa ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("toJpa()")
    class ToJpa {

        @Test
        @DisplayName("maps id correctly")
        void mapsId() {
            Staff staff = Staff.create("Alice Admin", "aalice", "$2a$hash", StaffRole.ADMIN);
            StaffJpaEntity entity = mapper.toJpa(staff);
            assertThat(entity.getId()).isEqualTo(staff.getId().value());
        }

        @Test
        @DisplayName("maps fullName correctly")
        void mapsFullName() {
            Staff staff = Staff.create("Bob Manager", "bmanager", "$2a$hash", StaffRole.MANAGER);
            assertThat(mapper.toJpa(staff).getFullName()).isEqualTo("Bob Manager");
        }

        @Test
        @DisplayName("maps username correctly")
        void mapsUsername() {
            Staff staff = Staff.create("Name", "uniqueuser", "$h", StaffRole.FRONT_DESK);
            assertThat(mapper.toJpa(staff).getUsername()).isEqualTo("uniqueuser");
        }

        @Test
        @DisplayName("maps passwordHash correctly")
        void mapsPasswordHash() {
            Staff staff = Staff.create("Name", "user", "$2a$12$secrethash", StaffRole.ACCOUNTANT);
            assertThat(mapper.toJpa(staff).getPasswordHash()).isEqualTo("$2a$12$secrethash");
        }

        @Test
        @DisplayName("maps role correctly for every StaffRole value")
        void mapsRoleForEveryValue() {
            for (StaffRole role : StaffRole.values()) {
                Staff staff = Staff.create("N", "u" + role.name(), "$h", role);
                assertThat(mapper.toJpa(staff).getRole()).isEqualTo(role);
            }
        }

        @Test
        @DisplayName("maps active = true for a new staff member")
        void mapsActiveTrue() {
            Staff staff = Staff.create("N", "u", "$h", StaffRole.FRONT_DESK);
            assertThat(mapper.toJpa(staff).isActive()).isTrue();
        }

        @Test
        @DisplayName("maps active = false for a deactivated staff member")
        void mapsActiveFalse() {
            StaffId id = StaffId.generate();
            Staff staff = Staff.reconstitute(id, "N", "u", "$h", StaffRole.FRONT_DESK,
                    false, LocalDateTime.now(), LocalDateTime.now());
            assertThat(mapper.toJpa(staff).isActive()).isFalse();
        }
    }

    // ── toDomain ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("toDomain()")
    class ToDomain {

        @Test
        @DisplayName("maps all fields from StaffJpaEntity to Staff")
        void mapsAllFields() {
            UUID id  = UUID.randomUUID();
            LocalDateTime now = LocalDateTime.now();

            StaffJpaEntity entity = StaffJpaEntity.builder()
                    .id(id)
                    .fullName("Citra Accountant")
                    .username("citra")
                    .passwordHash("$2a$12$ahash")
                    .role(StaffRole.ACCOUNTANT)
                    .active(true)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            Staff staff = mapper.toDomain(entity);

            assertThat(staff.getId().value()).isEqualTo(id);
            assertThat(staff.getFullName()).isEqualTo("Citra Accountant");
            assertThat(staff.getUsername()).isEqualTo("citra");
            assertThat(staff.getPasswordHash()).isEqualTo("$2a$12$ahash");
            assertThat(staff.getRole()).isEqualTo(StaffRole.ACCOUNTANT);
            assertThat(staff.isActive()).isTrue();
            assertThat(staff.getCreatedAt()).isEqualTo(now);
            assertThat(staff.getUpdatedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("maps active = false correctly")
        void mapsActiveFalse() {
            StaffJpaEntity entity = StaffJpaEntity.builder()
                    .id(UUID.randomUUID())
                    .fullName("N")
                    .username("u")
                    .passwordHash("$h")
                    .role(StaffRole.HOUSEKEEPING)
                    .active(false)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            assertThat(mapper.toDomain(entity).isActive()).isFalse();
        }
    }

    // ── Round-trip ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Round-trip: Staff -> toJpa() -> toDomain()")
    class RoundTrip {

        @Test
        @DisplayName("all fields survive a full round-trip")
        void allFieldsSurviveRoundTrip() {
            StaffId       id  = StaffId.generate();
            LocalDateTime ts  = LocalDateTime.of(2025, 3, 10, 9, 0);

            Staff original = Staff.reconstitute(id, "Dedi Front", "dedi",
                    "$2a$12$testhash", StaffRole.FRONT_DESK, true, ts, ts);

            Staff restored = mapper.toDomain(mapper.toJpa(original));

            assertThat(restored.getId().value()).isEqualTo(original.getId().value());
            assertThat(restored.getFullName()).isEqualTo(original.getFullName());
            assertThat(restored.getUsername()).isEqualTo(original.getUsername());
            assertThat(restored.getPasswordHash()).isEqualTo(original.getPasswordHash());
            assertThat(restored.getRole()).isEqualTo(original.getRole());
            assertThat(restored.isActive()).isEqualTo(original.isActive());
            assertThat(restored.getCreatedAt()).isEqualTo(original.getCreatedAt());
            assertThat(restored.getUpdatedAt()).isEqualTo(original.getUpdatedAt());
        }
    }
}
