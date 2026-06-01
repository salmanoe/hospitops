package id.co.hospitops.hotel.infrastructure.persistence;

import id.co.hospitops.hotel.domain.model.Hotel;
import id.co.hospitops.hotel.domain.model.HotelStatus;
import id.co.hospitops.hotel.domain.port.out.HotelRepository;
import id.co.hospitops.hotel.infrastructure.persistence.entity.HotelJpaEntity;
import id.co.hospitops.hotel.infrastructure.persistence.entity.SetupChecklistJpaEntity;
import id.co.hospitops.shared.GroupId;
import id.co.hospitops.shared.HotelId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
class HotelRepositoryImpl implements HotelRepository {

    private final HotelJpaRepository hotelJpa;
    private final SetupChecklistJpaRepository checklistJpa;
    private final HotelMapper mapper;

    /**
     * Persists the hotel and its setup checklist.
     *
     * <p>Uses a load-and-update pattern for both entities: if a row already exists,
     * the managed entity is updated in-place so that the {@code @Version} field is
     * preserved across saves. Creating a fresh detached entity on every call would
     * reset the version to {@code null}/0, causing Spring Data JPA to call
     * {@code persist()} on an existing row (first case) or an optimistic-lock mismatch
     * on the second update (second case).
     */
    @Override
    public Hotel save(Hotel hotel) {
        HotelJpaEntity hotelEntity = hotelJpa.findById(hotel.getId().value())
                .map(existing -> updateHotelFields(existing, hotel))
                .orElseGet(() -> mapper.toJpa(hotel));
        HotelJpaEntity savedHotel = hotelJpa.save(hotelEntity);

        SetupChecklistJpaEntity checklistEntity = checklistJpa.findById(hotel.getId().value())
                .map(existing -> updateChecklistFields(existing, hotel))
                .orElseGet(() -> mapper.toChecklistJpa(hotel.getId(), hotel.getChecklist()));
        SetupChecklistJpaEntity savedChecklist = checklistJpa.save(checklistEntity);

        return mapper.toDomain(savedHotel, savedChecklist);
    }

    private HotelJpaEntity updateHotelFields(HotelJpaEntity entity, Hotel hotel) {
        entity.setName(hotel.getName());
        entity.setAddress(hotel.getAddress());
        entity.setTimezone(hotel.getTimezone());
        entity.setCurrency(hotel.getCurrency());
        entity.setStarRating(hotel.getStarRating());
        entity.setDefaultCheckInTime(hotel.getDefaultCheckInTime());
        entity.setDefaultCheckOutTime(hotel.getDefaultCheckOutTime());
        entity.setStatus(hotel.getStatus());
        return entity;
    }

    private SetupChecklistJpaEntity updateChecklistFields(SetupChecklistJpaEntity entity, Hotel hotel) {
        entity.setProfileComplete(hotel.getChecklist().isProfileComplete());
        entity.setPolicyComplete(hotel.getChecklist().isPolicyComplete());
        entity.setRoomTypeAdded(hotel.getChecklist().isRoomTypeAdded());
        entity.setRoomAdded(hotel.getChecklist().isRoomAdded());
        entity.setStaffAccountCreated(hotel.getChecklist().isStaffAccountCreated());
        return entity;
    }

    @Override
    public Optional<Hotel> findById(HotelId id) {
        return hotelJpa.findById(id.value()).map(hotelEntity -> {
            SetupChecklistJpaEntity checklist =
                    checklistJpa.findById(id.value())
                            .orElseGet(() -> emptyChecklist(id));
            return mapper.toDomain(hotelEntity, checklist);
        });
    }

    @Override
    public List<Hotel> findByGroupId(GroupId groupId) {
        return hotelJpa.findByGroupId(groupId.value()).stream()
                .map(hotelEntity -> {
                    HotelId hotelId = HotelId.of(hotelEntity.getId());
                    SetupChecklistJpaEntity checklist =
                            checklistJpa.findById(hotelEntity.getId())
                                    .orElseGet(() -> emptyChecklist(hotelId));
                    return mapper.toDomain(hotelEntity, checklist);
                })
                .toList();
    }

    @Override
    public List<Hotel> findAll() {
        return hotelJpa.findAll().stream()
                .map(hotelEntity -> {
                    HotelId hotelId = HotelId.of(hotelEntity.getId());
                    SetupChecklistJpaEntity checklist =
                            checklistJpa.findById(hotelEntity.getId())
                                    .orElseGet(() -> emptyChecklist(hotelId));
                    return mapper.toDomain(hotelEntity, checklist);
                })
                .toList();
    }

    @Override
    public Optional<HotelStatus> findStatusById(HotelId id) {
        return hotelJpa.findStatusById(id.value());
    }

    @Override
    public Optional<HotelSnapshot> findSnapshotById(HotelId id) {
        return hotelJpa.findStatusAndGroupById(id.value())
                .map(view -> new HotelSnapshot(
                        view.getStatus(),
                        GroupId.of(view.getGroupId()),
                        view.getName()
                ));
    }

    private SetupChecklistJpaEntity emptyChecklist(HotelId hotelId) {
        return SetupChecklistJpaEntity.builder()
                .hotelId(hotelId.value())
                .profileComplete(false)
                .policyComplete(false)
                .roomTypeAdded(false)
                .roomAdded(false)
                .staffAccountCreated(false)
                .build();
    }
}
