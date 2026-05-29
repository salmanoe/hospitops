package id.co.hospitops.hotel.domain.model;

/** The five steps a GROUP_ADMIN must complete before a hotel can go ACTIVE. */
public enum SetupStep {
    /** Step 1 — name, address, timezone, currency, star rating. */
    PROFILE,
    /** Step 2 — default check-in/out times, tax policy, invoice branding. */
    POLICY,
    /** Step 3 — at least one room type defined. */
    ROOM_TYPE,
    /** Step 4 — at least one room linked to a room type. */
    ROOM,
    /** Step 5 — at least one ADMIN or MANAGER staff account created. */
    STAFF_ACCOUNT
}
