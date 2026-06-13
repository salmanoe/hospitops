-- ═══════════════════════════════════════════════════════════════
-- V20__add_version_to_group_tables.sql
-- Add optimistic-locking version columns to group and group_admin.
--
-- Architecture contract: every JPA entity with mutable fields must
-- carry a @Version column for optimistic concurrency control.
-- group.name is mutable; group_admin.password_hash is mutable.
-- ═══════════════════════════════════════════════════════════════

ALTER TABLE "group"
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE group_admin
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
