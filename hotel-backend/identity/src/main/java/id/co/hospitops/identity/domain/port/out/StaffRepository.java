package id.co.hospitops.identity.domain.port.out;

import id.co.hospitops.identity.domain.model.Staff;
import id.co.hospitops.shared.HotelId;
import id.co.hospitops.shared.StaffId;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface StaffRepository {
    Staff save(Staff staff);

    /**
     * Unscoped lookup — for use by the auth filter only (HotelContext is not yet bound).
     * Management operations must use {@link #findByIdInCurrentHotel(StaffId)} instead.
     */
    Optional<Staff> findById(StaffId id);

    /**
     * Hotel-scoped lookup — returns empty if the staff member does not belong to
     * the hotel currently bound in {@code HotelContext}. Use for all management
     * operations (update, password change, toggle) where HotelContext is bound.
     */
    Optional<Staff> findByIdInCurrentHotel(StaffId id);

    Optional<Staff> findByUsername(String username);

    /**
     * Hotel-scoped credential lookup for the hotel-aware login endpoint.
     * Does NOT require {@code HotelContext} to be bound — the hotelId comes
     * from the URL path parameter, not from {@code HotelContext}.
     */
    Optional<Staff> findByUsernameAndHotelId(String username, HotelId hotelId);

    boolean existsByUsername(String username);

    List<Staff> findAll(Pageable pageable);

    long count();
}
