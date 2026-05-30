-- ═══════════════════════════════════════════════════════════════
-- V16__fix_hotel_currency_type.sql
-- Converts hotel.currency from CHAR(3) to VARCHAR(3).
--
-- CHAR(3) is stored as bpchar in PostgreSQL and fails Hibernate's
-- schema validation, which maps a plain String field to VARCHAR.
-- VARCHAR(3) is semantically equivalent for fixed-length currency
-- codes (ISO-4217) and matches the JPA entity mapping.
-- ═══════════════════════════════════════════════════════════════

ALTER TABLE hotel
    ALTER COLUMN currency TYPE VARCHAR(3);
