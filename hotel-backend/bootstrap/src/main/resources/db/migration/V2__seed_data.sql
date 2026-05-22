-- ═══════════════════════════════════════════════════════════════
-- V2__seed_data.sql
-- Place in: bootstrap/src/main/resources/db/migration/
--
-- D1 FIX: BCrypt hashes generated at strength 12 using real bcrypt.
-- Python bcrypt and Spring BCryptPasswordEncoder produce compatible
-- hashes ($2b$ prefix is accepted by Spring Security as $2a$).
--
-- Default credentials:
--   admin        / admin123
--   manager      / manager123
--   frontdesk1   / frontdesk123
--   frontdesk2   / frontdesk123
--   housekeeper  / hk123456
--   accountant   / acc12345
--
-- To regenerate hashes:
--   mvn compile exec:java \
--     -Dexec.mainClass="com.hotel.identity.infrastructure.util.PasswordEncoderUtil" \
--     -pl identity
-- ═══════════════════════════════════════════════════════════════

-- ── Staff ─────────────────────────────────────────────────────────
INSERT INTO staff (id, full_name, username, password_hash, role, active)
VALUES (gen_random_uuid(), 'System Administrator', 'admin',
        '$2b$12$te0zOtNd9lbtvEH2sYLWJukOXbYBQ0xSKr4RG9f2cCc.2NCjGoOFW',
        'ADMIN', true),

       (gen_random_uuid(), 'Hotel Manager', 'manager',
        '$2b$12$WauttNT1vfNCx4rinbRTSOCdYKfmoEaSTokksYB82m2py4BHXSRt2',
        'MANAGER', true),

       (gen_random_uuid(), 'Budi Santoso', 'frontdesk1',
        '$2b$12$6ddvSvBFMU4gU5CRXVgC.u7BEmp70AXCu1G5.YDTxMkI5V1Oh6I3G',
        'FRONT_DESK', true),

       (gen_random_uuid(), 'Sari Dewi', 'frontdesk2',
        '$2b$12$ejCDt9m8.abR06HzYBwRQ.BVM9mZS4TD83qJ.fYXdvLgE7t/8VEQO',
        'FRONT_DESK', true),

       (gen_random_uuid(), 'Wati Rahayu', 'housekeeper',
        '$2b$12$H3zFPZ0lIVQPSBL9sgmB9uKLptEy/tmv5zrKCd9c5d9Lm9mamDwTi',
        'HOUSEKEEPING', true),

       (gen_random_uuid(), 'Andi Wijaya', 'accountant',
        '$2b$12$Wl0hs4K2T/PMOceB3HQr8.LU1lXsEytttYzXE5px.aZumMj.VtT3i',
        'ACCOUNTANT', true);


-- ── Room Types ────────────────────────────────────────────────────
-- Using fixed UUIDs so V3+ migrations can reference them safely
INSERT INTO room_type (id, name, capacity, description, base_price)
VALUES ('a1000000-0000-0000-0000-000000000001', 'Standard', 2,
        'Comfortable room with twin or queen bed, basic amenities.', 450000),

       ('a1000000-0000-0000-0000-000000000002', 'Superior', 2,
        'Larger room with city view and upgraded furnishings.', 600000),

       ('a1000000-0000-0000-0000-000000000003', 'Deluxe', 2,
        'Spacious room with premium bedding and pool or garden view.', 800000),

       ('a1000000-0000-0000-0000-000000000004', 'Junior Suite', 3,
        'Separate living area, king bed, and luxury bathroom.', 1200000),

       ('a1000000-0000-0000-0000-000000000005', 'Suite', 4,
        'Full suite with living room, dining area, and premium amenities.', 1800000),

       ('a1000000-0000-0000-0000-000000000006', 'Family Room', 4,
        'Two connected bedrooms ideal for families with children.', 1000000);


-- ── Room Rate Overrides (sample seasonal pricing) ────────────────
INSERT INTO room_rate_override (room_type_id, name, price_override, valid_from, valid_until)
VALUES ('a1000000-0000-0000-0000-000000000001', 'Year-End Peak', 540000, '2025-12-20', '2026-01-05'),
       ('a1000000-0000-0000-0000-000000000002', 'Year-End Peak', 720000, '2025-12-20', '2026-01-05'),
       ('a1000000-0000-0000-0000-000000000003', 'Year-End Peak', 960000, '2025-12-20', '2026-01-05'),
       ('a1000000-0000-0000-0000-000000000004', 'Year-End Peak', 1440000, '2025-12-20', '2026-01-05'),
       ('a1000000-0000-0000-0000-000000000005', 'Year-End Peak', 2160000, '2025-12-20', '2026-01-05'),
       ('a1000000-0000-0000-0000-000000000006', 'Year-End Peak', 1200000, '2025-12-20', '2026-01-05');


-- ── Rooms — 40 rooms across 4 floors ─────────────────────────────

-- Floor 1: Standard + Family (10 rooms)
INSERT INTO room (room_number, floor, status, room_type_id)
VALUES ('101', 1, 'AVAILABLE', 'a1000000-0000-0000-0000-000000000001'),
       ('102', 1, 'AVAILABLE', 'a1000000-0000-0000-0000-000000000001'),
       ('103', 1, 'AVAILABLE', 'a1000000-0000-0000-0000-000000000001'),
       ('104', 1, 'AVAILABLE', 'a1000000-0000-0000-0000-000000000001'),
       ('105', 1, 'AVAILABLE', 'a1000000-0000-0000-0000-000000000001'),
       ('106', 1, 'AVAILABLE', 'a1000000-0000-0000-0000-000000000001'),
       ('107', 1, 'AVAILABLE', 'a1000000-0000-0000-0000-000000000006'),
       ('108', 1, 'AVAILABLE', 'a1000000-0000-0000-0000-000000000006'),
       ('109', 1, 'AVAILABLE', 'a1000000-0000-0000-0000-000000000001'), -- W-10 FIX: 2 added rooms
       ('110', 1, 'AVAILABLE', 'a1000000-0000-0000-0000-000000000006');

-- Floor 2: Standard + Superior (10 rooms)
INSERT INTO room (room_number, floor, status, room_type_id)
VALUES ('201', 2, 'AVAILABLE', 'a1000000-0000-0000-0000-000000000001'),
       ('202', 2, 'AVAILABLE', 'a1000000-0000-0000-0000-000000000001'),
       ('203', 2, 'AVAILABLE', 'a1000000-0000-0000-0000-000000000001'),
       ('204', 2, 'AVAILABLE', 'a1000000-0000-0000-0000-000000000002'),
       ('205', 2, 'AVAILABLE', 'a1000000-0000-0000-0000-000000000002'),
       ('206', 2, 'AVAILABLE', 'a1000000-0000-0000-0000-000000000002'),
       ('207', 2, 'AVAILABLE', 'a1000000-0000-0000-0000-000000000002'),
       ('208', 2, 'AVAILABLE', 'a1000000-0000-0000-0000-000000000006'),
       ('209', 2, 'AVAILABLE', 'a1000000-0000-0000-0000-000000000002'), -- W-10 FIX: 2 added rooms
       ('210', 2, 'AVAILABLE', 'a1000000-0000-0000-0000-000000000002');

-- Floor 3: Superior + Deluxe (10 rooms)
INSERT INTO room (room_number, floor, status, room_type_id)
VALUES ('301', 3, 'AVAILABLE', 'a1000000-0000-0000-0000-000000000002'),
       ('302', 3, 'AVAILABLE', 'a1000000-0000-0000-0000-000000000002'),
       ('303', 3, 'AVAILABLE', 'a1000000-0000-0000-0000-000000000003'),
       ('304', 3, 'AVAILABLE', 'a1000000-0000-0000-0000-000000000003'),
       ('305', 3, 'AVAILABLE', 'a1000000-0000-0000-0000-000000000003'),
       ('306', 3, 'AVAILABLE', 'a1000000-0000-0000-0000-000000000003'),
       ('307', 3, 'AVAILABLE', 'a1000000-0000-0000-0000-000000000003'),
       ('308', 3, 'AVAILABLE', 'a1000000-0000-0000-0000-000000000003'),
       ('309', 3, 'AVAILABLE', 'a1000000-0000-0000-0000-000000000002'), -- W-10 FIX: 2 added rooms
       ('310', 3, 'AVAILABLE', 'a1000000-0000-0000-0000-000000000003');

-- Floor 4: Deluxe + Suites (10 rooms)
INSERT INTO room (room_number, floor, status, room_type_id)
VALUES ('401', 4, 'AVAILABLE', 'a1000000-0000-0000-0000-000000000003'),
       ('402', 4, 'AVAILABLE', 'a1000000-0000-0000-0000-000000000003'),
       ('403', 4, 'AVAILABLE', 'a1000000-0000-0000-0000-000000000003'),
       ('404', 4, 'AVAILABLE', 'a1000000-0000-0000-0000-000000000004'),
       ('405', 4, 'AVAILABLE', 'a1000000-0000-0000-0000-000000000004'),
       ('406', 4, 'AVAILABLE', 'a1000000-0000-0000-0000-000000000004'),
       ('407', 4, 'AVAILABLE', 'a1000000-0000-0000-0000-000000000005'),
       ('408', 4, 'AVAILABLE', 'a1000000-0000-0000-0000-000000000005'),
       ('409', 4, 'AVAILABLE', 'a1000000-0000-0000-0000-000000000004'), -- W-10 FIX: 2 added rooms
       ('410', 4, 'AVAILABLE', 'a1000000-0000-0000-0000-000000000005');
