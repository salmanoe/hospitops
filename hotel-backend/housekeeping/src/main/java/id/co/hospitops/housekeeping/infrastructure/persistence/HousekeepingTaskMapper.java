package id.co.hospitops.housekeeping.infrastructure.persistence;

import id.co.hospitops.housekeeping.domain.model.HousekeepingTask;
import id.co.hospitops.housekeeping.infrastructure.persistence.entity.HousekeepingTaskJpaEntity;
import id.co.hospitops.shared.*;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface HousekeepingTaskMapper {
    @Mapping(target = "roomId", expression = "java(t.getRoomId().value())")
    @Mapping(target = "reservationId", expression = "java(t.getReservationId() != null ? t.getReservationId().value() : null)")
    @Mapping(target = "assignedTo", expression = "java(t.getAssignedTo() != null ? t.getAssignedTo().value() : null)")
    HousekeepingTaskJpaEntity toJpa(HousekeepingTask t);

    // HousekeepingTask has a private constructor and no setters, so MapStruct cannot
    // construct it via its normal code-generation path. Use a default method calling
    // reconstitute() directly — same pattern as RoomMapper.toDomain(RoomRateOverrideJpaEntity).
    default HousekeepingTask toDomain(HousekeepingTaskJpaEntity e) {
        return HousekeepingTask.reconstitute(
                e.getId(),
                RoomId.of(e.getRoomId()),
                e.getReservationId() != null ? ReservationId.of(e.getReservationId()) : null,
                e.getAssignedTo()    != null ? StaffId.of(e.getAssignedTo())           : null,
                e.getNotes(),
                e.isCompleted(),
                e.getCompletedAt(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
