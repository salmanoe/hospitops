package id.co.hospitops.hotel.domain.model;

import id.co.hospitops.shared.HotelId;
import lombok.Getter;

/**
 * Tracks the five setup steps a hotel must complete before it can go ACTIVE.
 * This is a value object owned by {@link Hotel} — it has no independent lifecycle.
 */
@Getter
public class SetupChecklist {

    private final HotelId hotelId;
    private boolean profileComplete;
    private boolean policyComplete;
    private boolean roomTypeAdded;
    private boolean roomAdded;
    private boolean staffAccountCreated;

    public static SetupChecklist empty(HotelId hotelId) {
        return new SetupChecklist(hotelId, false, false, false, false, false);
    }

    public static SetupChecklist reconstitute(HotelId hotelId,
                                              boolean profileComplete,
                                              boolean policyComplete,
                                              boolean roomTypeAdded,
                                              boolean roomAdded,
                                              boolean staffAccountCreated) {
        return new SetupChecklist(hotelId, profileComplete, policyComplete,
                roomTypeAdded, roomAdded, staffAccountCreated);
    }

    private SetupChecklist(HotelId hotelId, boolean profileComplete, boolean policyComplete,
                           boolean roomTypeAdded, boolean roomAdded, boolean staffAccountCreated) {
        this.hotelId = hotelId;
        this.profileComplete = profileComplete;
        this.policyComplete = policyComplete;
        this.roomTypeAdded = roomTypeAdded;
        this.roomAdded = roomAdded;
        this.staffAccountCreated = staffAccountCreated;
    }

    /** Marks a step as complete. Idempotent — completing an already-complete step is safe. */
    public void complete(SetupStep step) {
        switch (step) {
            case PROFILE        -> profileComplete = true;
            case POLICY         -> policyComplete = true;
            case ROOM_TYPE      -> roomTypeAdded = true;
            case ROOM           -> roomAdded = true;
            case STAFF_ACCOUNT  -> staffAccountCreated = true;
        }
    }

    /** Returns {@code true} when all five steps are complete. */
    public boolean isComplete() {
        return profileComplete && policyComplete
                && roomTypeAdded && roomAdded && staffAccountCreated;
    }

    /** Returns a human-readable list of remaining steps. */
    public java.util.List<SetupStep> remainingSteps() {
        var remaining = new java.util.ArrayList<SetupStep>();
        if (!profileComplete)       remaining.add(SetupStep.PROFILE);
        if (!policyComplete)        remaining.add(SetupStep.POLICY);
        if (!roomTypeAdded)         remaining.add(SetupStep.ROOM_TYPE);
        if (!roomAdded)             remaining.add(SetupStep.ROOM);
        if (!staffAccountCreated)   remaining.add(SetupStep.STAFF_ACCOUNT);
        return java.util.Collections.unmodifiableList(remaining);
    }
}
