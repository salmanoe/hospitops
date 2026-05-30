package id.co.hospitops.hotel.infrastructure.persistence;

import id.co.hospitops.hotel.domain.model.Hotel;
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

    @Override
    public Hotel save(Hotel hotel) {
        HotelJpaEntity savedHotel = hotelJpa.save(mapper.toJpa(hotel));
        SetupChecklistJpaEntity savedChecklist =
                checklistJpa.save(mapper.toChecklistJpa(hotel.getChecklist()));
        return mapper.toDomain(savedHotel, savedChecklist);
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
