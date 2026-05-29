package id.co.hospitops.hotel.domain.model;

public enum HotelStatus {
    /** Hotel is being configured. Staff cannot log in. No reservations allowed. */
    SETUP,
    /** Hotel is fully operational. */
    ACTIVE,
    /** Hotel is temporarily offline. Existing data is preserved. */
    SUSPENDED
}
