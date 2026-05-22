package id.co.hospitops.guest.domain.port.in;

import id.co.hospitops.guest.application.command.*;
import id.co.hospitops.guest.application.response.*;
import id.co.hospitops.shared.GuestId;
import id.co.hospitops.shared.web.PageResult;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ManageGuestUseCase {
    GuestResponse register(RegisterGuestCommand command);

    GuestResponse update(GuestId id, UpdateGuestCommand command);

    GuestResponse findById(GuestId id);

    boolean existsById(GuestId id);

    PageResult<GuestResponse> search(String query, Pageable pageable);

    List<GuestSearchResult> quickSearch(String query);
}
