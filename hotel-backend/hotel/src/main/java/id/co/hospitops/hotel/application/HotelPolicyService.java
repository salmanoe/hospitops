package id.co.hospitops.hotel.application;

import id.co.hospitops.hotel.application.command.SavePolicyConfigCommand;
import id.co.hospitops.hotel.application.response.PolicyConfigResponse;
import id.co.hospitops.hotel.domain.model.HotelStatus;
import id.co.hospitops.hotel.domain.model.PolicyConfig;
import id.co.hospitops.hotel.domain.model.SetupStep;
import id.co.hospitops.hotel.domain.port.in.ManageHotelPolicyUseCase;
import id.co.hospitops.hotel.domain.port.out.HotelPolicyConfigRepository;
import id.co.hospitops.hotel.domain.port.out.HotelRepository;
import id.co.hospitops.shared.HotelId;
import id.co.hospitops.shared.event.HotelActivatedEvent;
import id.co.hospitops.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class HotelPolicyService implements ManageHotelPolicyUseCase {

    private final HotelPolicyConfigRepository policyRepo;
    private final HotelRepository hotelRepo;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Upserts the policy config for a hotel.
     *
     * <p>If the hotel is currently in {@code SETUP} status and the {@code POLICY} step was
     * not yet marked complete, this method marks it complete. If marking the step causes all
     * five steps to be done, the hotel transitions to {@code ACTIVE} and a
     * {@link HotelActivatedEvent} is published.
     *
     * <p>This is idempotent — calling it again with the same or updated values is safe.
     */
    @Override
    public PolicyConfigResponse savePolicyConfig(SavePolicyConfigCommand cmd) {
        PolicyConfig config = policyRepo.findByHotelId(cmd.hotelId())
                .map(existing -> {
                    existing.update(cmd.taxPercent(), cmd.taxName(),
                            cmd.invoiceHotelName(), cmd.invoiceAddress(), cmd.invoiceFooterNote());
                    return existing;
                })
                .orElseGet(() -> PolicyConfig.create(
                        cmd.hotelId(), cmd.taxPercent(), cmd.taxName(),
                        cmd.invoiceHotelName(), cmd.invoiceAddress(), cmd.invoiceFooterNote()));

        PolicyConfig saved = policyRepo.save(config);

        markPolicyStepIfInSetup(cmd.hotelId());

        log.info("Policy config saved for hotel {}", cmd.hotelId());
        return PolicyConfigResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PolicyConfigResponse findByHotelId(HotelId hotelId) {
        return policyRepo.findByHotelId(hotelId)
                .map(PolicyConfigResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "PolicyConfig", hotelId.value()));
    }

    /**
     * Marks the {@link SetupStep#POLICY} wizard step complete if the hotel is still in
     * {@code SETUP} and the step was not yet done. Publishes {@link HotelActivatedEvent}
     * if completing the step triggers auto-activation.
     */
    private void markPolicyStepIfInSetup(HotelId hotelId) {
        hotelRepo.findById(hotelId).ifPresent(hotel -> {
            if (hotel.getStatus() != HotelStatus.SETUP) return;
            if (hotel.getChecklist().isPolicyComplete()) return;

            boolean justActivated = hotel.completeSetupStep(SetupStep.POLICY);
            hotelRepo.save(hotel);

            if (justActivated) {
                eventPublisher.publishEvent(new HotelActivatedEvent(hotel.getId()));
                log.info("Hotel {} auto-activated after policy config saved", hotelId);
            }
        });
    }
}
