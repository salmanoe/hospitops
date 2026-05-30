package id.co.hospitops.room.application;

import id.co.hospitops.room.application.command.*;
import id.co.hospitops.room.application.response.*;
import id.co.hospitops.room.domain.model.*;
import id.co.hospitops.room.domain.port.in.ManageRoomTypeUseCase;
import id.co.hospitops.room.domain.port.out.*;
import id.co.hospitops.shared.*;
import id.co.hospitops.shared.HotelContext;
import id.co.hospitops.shared.exception.*;
import id.co.hospitops.shared.web.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * CQRS: handles room type CRUD and rate override management only.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class RoomTypeService implements ManageRoomTypeUseCase {
    private final RoomTypeRepository roomTypeRepo;
    private final RoomRateOverrideRepository overrideRepo;

    @Override
    public RoomTypeResponse createRoomType(CreateRoomTypeCommand cmd) {
        if (roomTypeRepo.existsByName(cmd.name()))
            throw new ConflictException("Room type already exists: " + cmd.name());
        return RoomTypeResponse.from(roomTypeRepo.save(
                RoomType.create(HotelContext.current(), cmd.name(), cmd.capacity(),
                        cmd.description(), Money.of(cmd.basePrice()))));
    }

    @Override
    public RoomTypeResponse updateRoomType(RoomTypeId id, UpdateRoomTypeCommand cmd) {
        RoomType rt = findRoomType(id);
        rt.update(cmd.name(), cmd.capacity(), cmd.description(), Money.of(cmd.basePrice()));
        return RoomTypeResponse.from(roomTypeRepo.save(rt));
    }

    @Override
    @Transactional(readOnly = true)
    public RoomTypeResponse findRoomTypeById(RoomTypeId id) {
        return RoomTypeResponse.from(findRoomType(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<RoomTypeResponse> findAllRoomTypes(Pageable pageable) {
        return PageResult.of(roomTypeRepo.findAll(pageable).stream().map(RoomTypeResponse::from).toList(),
                pageable.getPageNumber(), pageable.getPageSize(), roomTypeRepo.count());
    }

    @Override
    public RoomTypeResponse addRateOverride(RoomTypeId id, AddRateOverrideCommand cmd) {
        RoomType rt = findRoomType(id);
        overrideRepo.save(new RoomRateOverride(UUID.randomUUID(), id, cmd.name(),
                Money.of(cmd.priceOverride()), cmd.validFrom(), cmd.validUntil()));
        return RoomTypeResponse.from(rt);
    }

    RoomType findRoomType(RoomTypeId id) {
        return roomTypeRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RoomType", id.value()));
    }
}
