package id.co.hospitops.guest.application;

import id.co.hospitops.guest.application.command.RegisterGuestCommand;
import id.co.hospitops.guest.application.response.GuestResponse;
import id.co.hospitops.shared.HotelContext;
import id.co.hospitops.shared.HotelId;
import id.co.hospitops.guest.domain.model.Guest;
import id.co.hospitops.guest.domain.port.out.GuestRepository;
import id.co.hospitops.shared.exception.ConflictException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GuestService")
class GuestServiceTest {

    @Mock
    GuestRepository guestRepo;
    GuestService service;

    @BeforeEach
    void setUp() {
        service = new GuestService(guestRepo);
    }

    @Test
    @DisplayName("registers guest with unique ID number")
    void registersWithUniqueId() {
        var cmd = new RegisterGuestCommand("John", "P123", "ID", null, null, null);
        given(guestRepo.existsByIdNumber("P123")).willReturn(false);
        given(guestRepo.save(any())).willAnswer(inv -> inv.getArgument(0));
        GuestResponse[] holder = new GuestResponse[1];
        ScopedValue.where(HotelContext.HOTEL_ID, HotelId.generate())
                .run(() -> holder[0] = service.register(cmd));
        assertThat(holder[0].fullName()).isEqualTo("John");
        assertThat(holder[0].idNumber()).isEqualTo("P123");
    }

    @Test
    @DisplayName("registers guest without ID number (walk-in)")
    void registersWithoutIdNumber() {
        var cmd = new RegisterGuestCommand("Jane", null, null, null, null, null);
        given(guestRepo.save(any())).willAnswer(inv -> inv.getArgument(0));
        GuestResponse[] holder = new GuestResponse[1];
        ScopedValue.where(HotelContext.HOTEL_ID, HotelId.generate())
                .run(() -> holder[0] = service.register(cmd));
        assertThat(holder[0].fullName()).isEqualTo("Jane");
        then(guestRepo).should(never()).existsByIdNumber(any());
    }

    @Test
    @DisplayName("search with null query returns all guests and uses count() for total")
    void searchWithNullQueryReturnsAll() {
        // Regression: passing null to the JPQL search query produced CONCAT('%', NULL, '%')
        // which matched nothing — guests list appeared empty after registration.
        var pageable = PageRequest.of(0, 20, Sort.by("fullName"));
        var guest = Guest.create(HotelId.generate(), "Alice", null, null, null, null, null);
        given(guestRepo.search(null, pageable)).willReturn(List.of(guest));
        given(guestRepo.count()).willReturn(1L);

        var result = service.search(null, pageable);

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1L);
        then(guestRepo).should(never()).countByQuery(any());
    }

    @Test
    @DisplayName("search with blank query returns all guests and uses count() for total")
    void searchWithBlankQueryReturnsAll() {
        var pageable = PageRequest.of(0, 20, Sort.by("fullName"));
        var guest = Guest.create(HotelId.generate(), "Bob", null, null, null, null, null);
        given(guestRepo.search("", pageable)).willReturn(List.of(guest));
        given(guestRepo.count()).willReturn(1L);

        var result = service.search("", pageable);

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1L);
        then(guestRepo).should(never()).countByQuery(any());
    }

    @Test
    @DisplayName("throws ConflictException for duplicate ID number")
    void throwsForDuplicateIdNumber() {
        given(guestRepo.existsByIdNumber("DUP123")).willReturn(true);
        assertThatThrownBy(() ->
                service.register(new RegisterGuestCommand("X", "DUP123", null, null, null, null))
        ).isInstanceOf(ConflictException.class);
        then(guestRepo).should(never()).save(any());
    }
}
