package id.co.hospitops.hotel.domain.port.in;

import id.co.hospitops.hotel.application.command.SavePolicyConfigCommand;
import id.co.hospitops.hotel.application.response.PolicyConfigResponse;
import id.co.hospitops.shared.HotelId;

public interface ManageHotelPolicyUseCase {

    /**
     * Creates or updates the policy configuration for a hotel.
     * If the hotel is in {@code SETUP} status and the {@code POLICY} step was not yet
     * complete, this call marks it complete — potentially triggering auto-activation
     * if all other steps are also done.
     *
     * @return the saved policy config
     */
    PolicyConfigResponse savePolicyConfig(SavePolicyConfigCommand cmd);

    /**
     * Returns the current policy config for a hotel.
     *
     * @throws id.co.hospitops.shared.exception.ResourceNotFoundException if no config has been saved yet
     */
    PolicyConfigResponse findByHotelId(HotelId hotelId);
}
