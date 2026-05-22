-- Guest name is denormalised onto the invoice so the list and detail endpoints
-- can surface it without a cross-service call per row (avoids N+1).
-- The value is frozen at invoice-creation time, matching the behaviour of
-- reservation_number which is already stored the same way.

ALTER TABLE invoice
    ADD COLUMN guest_name VARCHAR(255);
