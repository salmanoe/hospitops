package id.co.hospitops.identity.application;

import id.co.hospitops.identity.application.command.*;
import id.co.hospitops.identity.application.response.StaffResponse;
import id.co.hospitops.identity.domain.model.Staff;
import id.co.hospitops.identity.domain.port.in.ManageStaffUseCase;
import id.co.hospitops.identity.domain.port.out.StaffRepository;
import id.co.hospitops.shared.StaffId;
import id.co.hospitops.shared.exception.*;
import id.co.hospitops.shared.web.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StaffService implements ManageStaffUseCase {

    private final StaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public StaffResponse createStaff(CreateStaffCommand command) {
        if (staffRepository.existsByUsername(command.username()))
            throw new ConflictException("Username already exists: " + command.username());

        Staff staff = Staff.create(
                command.fullName(),
                command.username(),
                passwordEncoder.encode(command.password()),
                command.role()
        );
        return StaffResponse.from(staffRepository.save(staff));
    }

    @Override
    @Transactional
    public StaffResponse updateStaff(StaffId id, UpdateStaffCommand command) {
        Staff staff = findStaff(id);
        staff.updateProfile(command.fullName());
        staff.changeRole(command.role());
        return StaffResponse.from(staffRepository.save(staff));
    }

    @Override
    @Transactional
    public void changePassword(StaffId id, ChangePasswordCommand command) {
        Staff staff = findStaff(id);
        if (!passwordEncoder.matches(command.currentPassword(), staff.getPasswordHash()))
            throw new BusinessRuleViolationException("Current password is incorrect");
        staff.changePassword(passwordEncoder.encode(command.newPassword()));
        staffRepository.save(staff);
    }

    @Override
    @Transactional
    public void toggleActive(StaffId id) {
        Staff staff = findStaff(id);
        if (staff.isActive()) staff.deactivate();
        else staff.activate();
        staffRepository.save(staff);
    }

    @Override
    @Transactional(readOnly = true)
    public StaffResponse findById(StaffId id) {
        return StaffResponse.from(findStaff(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<StaffResponse> findAll(Pageable pageable) {
        List<StaffResponse> list = staffRepository.findAll(pageable)
                .stream().map(StaffResponse::from).toList();
        return PageResult.of(list, pageable.getPageNumber(),
                pageable.getPageSize(), staffRepository.count());
    }

    private Staff findStaff(StaffId id) {
        return staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", id.value()));
    }
}
