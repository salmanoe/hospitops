package id.co.hospitops.hotel.domain.model;

import lombok.Getter;

/**
 * Tracks the five setup steps a hotel must complete before it can go ACTIVE.
 *
 * <p>This is a value object owned by {@link Hotel} — it has no independent lifecycle
 * and therefore carries no identity ({@code HotelId}). The owning Hotel is responsible
 * for associating this checklist with the correct hotel when persisting.
 */
@Getter
public class SetupChecklist {

    private boolean profileComplete;
    private boolean policyComplete;
    private boolean roomTypeAdded;
    private boolean roomAdded;
    private boolean staffAccountCreated;

    public static SetupChecklist empty() {
        return new SetupChecklist(false, false, false, false, false);
    }

    public static SetupChecklist reconstitute(boolean profileComplete,
                                              boolean policyComplete,
                                              boolean roomTypeAdded,
                                              boolean roomAdded,
                                              boolean staffAccountCreated) {
        return new SetupChecklist(profileComplete, policyComplete,
                roomTypeAdded, roomAdded, staffAccountCreated);
    }

    private SetupChecklist(boolean profileComplete, boolean policyComplete,
                           boolean roomTypeAdded, boolean roomAdded, boolean staffAccountCreated) {
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

    /** Returns an unmodifiable list of steps not yet completed. */
    public java.util.List<SetupStep> remainingSteps() {
        var remaining = new java.util.ArrayList<SetupStep>();
        if (!profileComplete)       remaining.add(SetupStep.PROFILE);
        if (!policyComplete)        remaining.add(SetupStep.POLICY);
        if (!roomTypeAdded)         remaining.add(SetupStep.ROOM_TYPE);
        if (!roomAdded)             remaining.add(SetupStep.ROOM);
        if (!staffAccountCreated)   remaining.add(SetupStep.STAFF_ACCOUNT);
        return java.util.List.copyOf(remaining);
    }
}
