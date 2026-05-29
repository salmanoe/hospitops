package id.co.hospitops.shared;

/**
 * Carries the current hotel scope for a virtual-thread request.
 *
 * <p>Uses {@link ScopedValue} rather than {@code ThreadLocal} — per ARCHITECTURE.md §7,
 * {@code ThreadLocal} must never be used with virtual threads because a virtual thread
 * may be remounted on a different carrier thread between suspensions.
 *
 * <p>The interceptor in {@code bootstrap} binds {@link #HOTEL_ID} at the start of every
 * hotel-scoped request via {@code ScopedValue.where(HotelContext.HOTEL_ID, hotelId).run(...)}.
 * All downstream code (services, repository adapters) calls {@link #current()} to read it.
 *
 * <p>GROUP_ADMIN requests without a hotel-scoped JWT will have this value unbound.
 * Use {@link #isBound()} before calling {@link #current()} in code that may run in
 * either context.
 */
public final class HotelContext {

    /** The scoped value slot — one per request, immutable once bound. */
    public static final ScopedValue<HotelId> HOTEL_ID = ScopedValue.newInstance();

    private HotelContext() {
        throw new UnsupportedOperationException("utility class");
    }

    /**
     * Returns the {@link HotelId} bound to the current scope.
     *
     * @return the current hotel ID
     * @throws IllegalStateException if no hotel context is bound to the current scope;
     *                               this indicates a programming error — a hotel-scoped
     *                               operation was invoked without a hotel JWT claim
     */
    public static HotelId current() {
        return HOTEL_ID.orElseThrow(() ->
                new IllegalStateException(
                        "No hotel context bound to the current scope. "
                        + "Ensure the request carries a hotel-scoped JWT."));
    }

    /**
     * Returns {@code true} if a hotel context is bound to the current scope.
     *
     * <p>Use this to distinguish GROUP_ADMIN group-level requests (unbound) from
     * hotel-scoped requests (bound) when a single code path must handle both.
     */
    public static boolean isBound() {
        return HOTEL_ID.isBound();
    }
}
