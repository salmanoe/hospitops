package id.co.hospitops.channel.infrastructure.connector;

import id.co.hospitops.channel.domain.model.AriUpdate;
import id.co.hospitops.channel.domain.port.out.ChannelConnectorException;
import id.co.hospitops.channel.domain.port.out.ChannelConnectorPort;
import id.co.hospitops.channel.infrastructure.config.ChannexProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Channex implementation of {@link ChannelConnectorPort}.
 *
 * <p>NOTE (verify against the Channex sandbox before go-live): the endpoint
 * paths ({@code /availability}, {@code /restrictions}), the {@code values}
 * envelope and the field names below follow the documented Channex ARI API,
 * but the exact shape should be confirmed against a live staging property once
 * an API key is available. They are deliberately isolated to this class.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChannexConnector implements ChannelConnectorPort {

    private final RestClient channexRestClient;
    private final ChannexProperties props;

    @Override
    public void pushAri(String externalPropertyId, List<AriUpdate> updates) {
        if (!props.isEnabled() || props.getApiKey().isBlank()) {
            throw new ChannelConnectorException(
                    "Channex connector disabled or API key missing (set channex.enabled + channex.api-key)");
        }
        // Channex rejects past dates — only send today onward.
        LocalDate today = LocalDate.now();
        List<AriUpdate> future = updates.stream()
                .filter(u -> !u.date().isBefore(today))
                .toList();
        if (future.isEmpty()) return;

        // Availability (per room type) and restrictions/rates (per rate plan) are
        // SEPARATE Channex endpoints. Rate is an integer in minor units.
        post("/availability", Map.of("values", future.stream()
                .map(u -> Map.<String, Object>of(
                        "property_id", externalPropertyId,
                        "room_type_id", u.externalRoomTypeId(),
                        "date", u.date().toString(),
                        "availability", u.availability()))
                .toList()));

        post("/restrictions", Map.of("values", future.stream()
                .map(u -> Map.<String, Object>of(
                        "property_id", externalPropertyId,
                        "rate_plan_id", u.externalRatePlanId(),
                        "date", u.date().toString(),
                        "rate", u.rate()))
                .toList()));

        log.debug("Pushed {} ARI night(s) to Channex property {}", future.size(), externalPropertyId);
    }

    private void post(String path, Object body) {
        try {
            channexRestClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw new ChannelConnectorException(
                    "Channex " + path + " returned " + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new ChannelConnectorException("Channex " + path + " transport error: " + e.getMessage(), e);
        }
    }
}
