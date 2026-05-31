-- Phase 8: Add optimistic-locking version column to hotel_setup_checklist.
-- Required by the architecture contract: every mutable JPA @Entity must have @Version.
ALTER TABLE hotel_setup_checklist
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
