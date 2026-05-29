-- ═══════════════════════════════════════════════════════════════
-- V10__create_group_tables.sql
-- Phase 2: Multi-hotel — group and group_admin tables
--
-- A Group is a hotel chain or management company.
-- A GroupAdmin is a super-user with cross-hotel visibility.
-- GroupAdmin credentials are BCrypt-hashed — same pattern as staff.
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE "group"
(
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(200) NOT NULL,
    owner_email VARCHAR(150) NOT NULL UNIQUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_group_owner_email ON "group" (owner_email);

CREATE TABLE group_admin
(
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    -- Cross-module reference to group — real FK in Stage 1 (monolith).
    -- Becomes a logical reference in Stage 3 (microservices). [tech-debt]
    group_id      UUID         NOT NULL REFERENCES "group" (id),
    email         VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_group_admin_group_id ON group_admin (group_id);
CREATE INDEX idx_group_admin_email    ON group_admin (email);
