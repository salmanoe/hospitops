package id.co.hospitops.identity.domain.model;

import org.hibernate.annotations.Struct;

@Struct(name = "staff_role")
public enum StaffRole {
    ADMIN,
    MANAGER,
    FRONT_DESK,
    HOUSEKEEPING,
    ACCOUNTANT
}
