package id.co.hospitops.guest.application;

import id.co.hospitops.guest.application.command.*;
import id.co.hospitops.guest.application.response.*;
import id.co.hospitops.guest.domain.model.Guest;
import id.co.hospitops.guest.domain.port.in.ManageGuestUseCase;
import id.co.hospitops.guest.domain.port.out.GuestRepository;
import id.co.hospitops.shared.GuestId;
import id.co.hospitops.shared.HotelContext;
import id.co.hospitops.shared.exception.*;
import id.co.hospitops.shared.web.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class GuestService implements ManageGuestUseCase {

    private final GuestRepository guestRepo;

    @Override
    public GuestResponse register(RegisterGuestCommand cmd) {
        if (cmd.idNumber() != null && !cmd.idNumber().isBlank()
                && guestRepo.existsByIdNumber(cmd.idNumber())) {
            throw new ConflictException("Guest with ID number already exists: " + cmd.idNumber());
        }
        Guest guest = Guest.create(HotelContext.current(), cmd.fullName(), cmd.idNumber(),
                cmd.nationality(), cmd.phone(), cmd.email(), cmd.address());
        return GuestResponse.from(guestRepo.save(guest));
    }

    @Override
    public GuestResponse update(GuestId id, UpdateGuestCommand cmd) {
        Guest guest = findGuest(id);
        guest.updateProfile(cmd.fullName(), cmd.nationality(),
                cmd.phone(), cmd.email(), cmd.address());
        return GuestResponse.from(guestRepo.save(guest));
    }

    @Override
    @Transactional(readOnly = true)
    public GuestResponse findById(GuestId id) {
        return GuestResponse.from(findGuest(id));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(GuestId id) {
        return guestRepo.findById(id).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<GuestResponse> search(String query, Pageable pageable) {
        List<GuestResponse> list = guestRepo.search(query, pageable)
                .stream().map(GuestResponse::from).toList();
        long total = (query != null && !query.isBlank())
                ? guestRepo.countByQuery(query) : guestRepo.count();
        return PageResult.of(list, pageable.getPageNumber(),
                pageable.getPageSize(), total);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GuestSearchResult> quickSearch(String query) {
        return guestRepo.quickSearch(query, 8)
                .stream().map(GuestSearchResult::from).toList();
    }

    private Guest findGuest(GuestId id) {
        return guestRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Guest", id.value()));
    }
}
