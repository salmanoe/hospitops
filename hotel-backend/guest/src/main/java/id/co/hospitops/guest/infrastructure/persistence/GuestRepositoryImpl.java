package id.co.hospitops.guest.infrastructure.persistence;

import id.co.hospitops.guest.domain.model.Guest;
import id.co.hospitops.guest.domain.port.out.GuestRepository;
import id.co.hospitops.shared.GuestId;
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
        return jpa.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<Guest> findByIdNumber(String n) {
        return jpa.findByIdNumber(n).map(mapper::toDomain);
    }

    @Override
    public boolean existsByIdNumber(String n) {
        return jpa.existsByIdNumber(n);
    }

    @Override
    public long count() {
        return jpa.count();
    }

    @Override
    public long countByQuery(String q) {
        return jpa.countByQuery(q);
    }

    @Override
    public List<Guest> search(String q, Pageable pageable) {
        if (q == null || q.isBlank()) {
            return jpa.findAll(pageable).getContent().stream().map(mapper::toDomain).toList();
        }
        return jpa.search(q, pageable).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Guest> quickSearch(String q, int limit) {
        return jpa.search(q, PageRequest.of(0, limit)).stream().map(mapper::toDomain).toList();
    }
}
