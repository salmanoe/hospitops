-- Phase 7: Pre-computed hotel summary table for the group dashboard.
-- Updated incrementally via domain event listeners and recomputed nightly by a
-- scheduled reconciliation job as a correctness safety net.

CREATE TABLE hotel_summary (
    hotel_id         UUID        PRIMARY KEY REFERENCES hotel(id),
    occupied_rooms   INT         NOT NULL DEFAULT 0,
    total_rooms      INT         NOT NULL DEFAULT 0,
    arrivals_today   INT         NOT NULL DEFAULT 0,
    departures_today INT         NOT NULL DEFAULT 0,
    revenue_today    NUMERIC(12, 2) NOT NULL DEFAULT 0,
    revenue_month    NUMERIC(12, 2) NOT NULL DEFAULT 0,
    dirty_rooms      INT         NOT NULL DEFAULT 0,
    updated_at       TIMESTAMP   NOT NULL DEFAULT now()
);

-- Seed a row for every existing hotel so the dashboard never returns gaps.
INSERT INTO hotel_summary (hotel_id)
SELECT id FROM hotel;
