package id.co.hospitops.hotel.domain.model;

import id.co.hospitops.shared.GroupId;
import id.co.hospitops.shared.Guard;
import id.co.hospitops.shared.HotelId;
import id.co.hospitops.shared.exception.BusinessRuleViolationException;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Aggregate root for a hotel within a group.
 *
 * <p>Lifecycle: {@code SETUP → ACTIVE → SUSPENDED}
 * The SETUP → ACTIVE transition is automatic when the {@link SetupChecklist} is complete.
 * All other transitions are explicit GROUP_ADMIN actions.
 */
@Getter
public class Hotel {

    private final HotelId id;
    private final GroupId groupId;
    private String name;
    private String address;
    private String timezone;
    private String currency;
    private int starRating;
    private LocalTime defaultCheckInTime;
    private LocalTime defaultCheckOutTime;
    private HotelStatus status;
    private SetupChecklist checklist;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static Hotel create(GroupId groupId, String name) {
        Guard.notNull(groupId, "GroupId");
        Guard.notBlank(name, "Hotel name");
        LocalDateTime now = LocalDateTime.now();
        HotelId id = HotelId.generate();
        return new Hotel(id, groupId, name,
                null, "UTC", "IDR", 3,
                LocalTime.of(14, 0), LocalTime.of(12, 0),
                HotelStatus.SETUP, SetupChecklist.empty(),
                now, now);
    }

    public static Hotel reconstitute(HotelId id, GroupId groupId, String name,
                                     String address, String timezone, String currency,
                                     int starRating, LocalTime defaultCheckInTime,
                                     LocalTime defaultCheckOutTime, HotelStatus status,
                                     SetupChecklist checklist,
                                     LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Hotel(id, groupId, name, address, timezone, currency,
                starRating, defaultCheckInTime, defaultCheckOutTime,
                status, checklist, createdAt, updatedAt);
    }

    private Hotel(HotelId id, GroupId groupId, String name,
                  String address, String timezone, String currency,
                  int starRating, LocalTime defaultCheckInTime,
                  LocalTime defaultCheckOutTime, HotelStatus status,
                  SetupChecklist checklist,
                  LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.groupId = groupId;
        this.name = name;
        this.address = address;
        this.timezone = timezone;
        this.currency = currency;
        this.starRating = starRating;
        this.defaultCheckInTime = defaultCheckInTime;
        this.defaultCheckOutTime = defaultCheckOutTime;
        this.status = status;
        this.checklist = checklist;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Marks a setup step complete. If all steps are now done and the hotel is still in
     * {@code SETUP}, it automatically transitions to {@code ACTIVE}.
     *
     * @return {@code true} if this call caused the hotel to become ACTIVE
     * @throws BusinessRuleViolationException if the hotel is not in SETUP status
     */
    public boolean completeSetupStep(SetupStep step) {
        if (status != HotelStatus.SETUP) {
            throw new BusinessRuleViolationException(
                    "Setup steps can only be completed while the hotel is in SETUP status");
        }
        checklist.complete(step);
        if (checklist.isComplete()) {
            status = HotelStatus.ACTIVE;
            updatedAt = LocalDateTime.now();
            return true;
        }
        updatedAt = LocalDateTime.now();
        return false;
    }

    /**
     * Suspends the hotel. Only allowed from ACTIVE status.
     *
     * @throws BusinessRuleViolationException if the hotel is not ACTIVE
     */
    public void suspend() {
        if (status != HotelStatus.ACTIVE) {
            throw new BusinessRuleViolationException(
                    "Only an ACTIVE hotel can be suspended");
        }
        status = HotelStatus.SUSPENDED;
        updatedAt = LocalDateTime.now();
    }

    /**
     * Reactivates the hotel. Only allowed from SUSPENDED status.
     *
     * @throws BusinessRuleViolationException if the hotel is not SUSPENDED
     */
    public void reactivate() {
        if (status != HotelStatus.SUSPENDED) {
            throw new BusinessRuleViolationException(
                    "Only a SUSPENDED hotel can be reactivated");
        }
        status = HotelStatus.ACTIVE;
        updatedAt = LocalDateTime.now();
    }

    /** Updates hotel profile fields (Step 1 of the setup wizard). */
    public void updateProfile(String name, String address, String timezone,
                              String currency, int starRating) {
        Guard.notBlank(name, "Hotel name");
        Guard.notBlank(timezone, "Timezone");
        Guard.notBlank(currency, "Currency");
        this.name = name;
        this.address = address;
        this.timezone = timezone;
        this.currency = currency;
        this.starRating = starRating;
        this.updatedAt = LocalDateTime.now();
    }

    /** Updates hotel policy fields (Step 2 of the setup wizard). */
    public void updatePolicy(LocalTime defaultCheckInTime, LocalTime defaultCheckOutTime) {
        Guard.notNull(defaultCheckInTime, "Default check-in time");
        Guard.notNull(defaultCheckOutTime, "Default check-out time");
        this.defaultCheckInTime = defaultCheckInTime;
        this.defaultCheckOutTime = defaultCheckOutTime;
        this.updatedAt = LocalDateTime.now();
    }
}
