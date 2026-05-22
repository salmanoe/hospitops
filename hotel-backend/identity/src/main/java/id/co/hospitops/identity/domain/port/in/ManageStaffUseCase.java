package id.co.hospitops.identity.domain.port.in;

import id.co.hospitops.identity.application.command.*;
import id.co.hospitops.identity.application.response.StaffResponse;
import id.co.hospitops.shared.StaffId;
import id.co.hospitops.shared.web.PageResult;
import org.springframework.data.domain.Pageable;

public interface ManageStaffUseCase {
    StaffResponse createStaff(CreateStaffCommand command);

    StaffResponse updateStaff(StaffId id, UpdateStaffCommand command);

    void changePassword(StaffId id, ChangePasswordCommand command);

    void toggleActive(StaffId id);

    StaffResponse findById(StaffId id);

    PageResult<StaffResponse> findAll(Pageable pageable);
}
