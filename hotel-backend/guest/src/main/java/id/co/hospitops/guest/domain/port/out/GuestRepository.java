package id.co.hospitops.guest.domain.port.out;

import id.co.hospitops.guest.domain.model.Guest;
import id.co.hospitops.shared.GuestId;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface GuestRepository {
    Guest save(Guest guest);

    Optional<Guest> findById(GuestId id);

    Optional<Guest> findByIdNumber(String idNumber);

    List<Guest> search(String query, Pageable pageable);

    List<Guest> quickSearch(String query, int limit);

    boolean existsByIdNumber(String idNumber);

    long countByQuery(String query);

    long count();
}
