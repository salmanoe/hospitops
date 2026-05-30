package id.co.hospitops.guest.infrastructure.persistence;

import id.co.hospitops.guest.domain.model.Guest;
import id.co.hospitops.guest.domain.port.out.GuestRepository;
import id.co.hospitops.shared.GuestId;
import id.co.hospitops.shared.HotelContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// R-17 FIX: GuestRepositoryImpl no longer contains private toJpa/toDomain methods.
// Conversion is delegated to GuestMapper, which is independently testable and
// consistent with the RoomMapper pattern used in the room module.
@Repository
@RequiredArgsConstructor
public class GuestRepositoryImpl implements GuestRepository {

    private final GuestJpaRepository jpa;
    private final GuestMapper mapper;

    @Override
    public Guest save(Guest g) {
        return mapper.toDomain(jpa.save(mapper.toJpa(g)));
    }

    @Override
    public Optional<Guest> findById(GuestId id) {
        return jpa.findByIdAndHotelId(id.value(), HotelContext.current().value())
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Guest> findByIdNumber(String n) {
        return jpa.findByIdNumberAndHotelId(n, HotelContext.current().value())
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByIdNumber(String n) {
        return jpa.existsByIdNumberAndHotelId(n, HotelContext.current().value());
    }

    @Override
    public long count() {
        return jpa.countByHotelId(HotelContext.current().value());
    }

    @Override
    public long countByQuery(String q) {
        return jpa.countByHotelIdAndQuery(HotelContext.current().value(), q);
    }

    @Override
    public List<Guest> search(String q, Pageable pageable) {
        var hotelId = HotelContext.current().value();
        if (q == null || q.isBlank()) {
            return jpa.findByHotelId(hotelId, pageable).stream().map(mapper::toDomain).toList();
        }
        return jpa.searchByHotelId(hotelId, q, pageable).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Guest> quickSearch(String q, int limit) {
        return jpa.searchByHotelId(HotelContext.current().value(), q, PageRequest.of(0, limit))
                .stream().map(mapper::toDomain).toList();
    }
}
