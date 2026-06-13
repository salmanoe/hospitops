package id.co.hospitops.shared.event;

import id.co.hospitops.shared.GroupId;
import id.co.hospitops.shared.HotelId;
import lombok.Getter;

/**
 * Published when a GROUP_ADMIN creates a new hotel within a group.
 * The hotel starts in {@code SETUP} status and is not yet operational.
 */
@Getter
public class HotelCreatedEvent extends DomainEvent {
    private final HotelId hotelId;
    private final GroupId groupId;

    public HotelCreatedEvent(HotelId hotelId, GroupId groupId) {
        super();
        this.hotelId = hotelId;
        this.groupId = groupId;
    }
}
