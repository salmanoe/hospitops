package id.co.hospitops.channel.infrastructure.connector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.co.hospitops.channel.domain.model.AriUpdate;
import id.co.hospitops.channel.domain.model.BookingRevision;
import id.co.hospitops.channel.domain.model.RevisionStatus;
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
import java.util.ArrayList;
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
    private final ObjectMapper objectMapper;

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

    // ── Inbound: booking revisions feed ─────────────────────────────────

    @Override
    public List<BookingRevision> fetchRevisionFeed() {
        if (!props.isEnabled() || props.getApiKey().isBlank()) {
            return List.of();
        }
        try {
            String body = channexRestClient.get()
                    .uri("/booking_revisions/feed")
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(body == null ? "{}" : body);
            List<BookingRevision> revisions = new ArrayList<>();
            for (JsonNode item : root.path("data")) {
                revisions.add(parseRevision(item));
            }
            return revisions;
        } catch (RestClientResponseException e) {
            throw new ChannelConnectorException(
                    "Channex feed returned " + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new ChannelConnectorException("Channex feed transport error: " + e.getMessage(), e);
        }
    }

    @Override
    public void ackRevision(String revisionId) {
        try {
            channexRestClient.post()
                    .uri("/booking_revisions/{id}/ack", revisionId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of())
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw new ChannelConnectorException(
                    "Channex ack " + revisionId + " returned " + e.getStatusCode() + ": "
                            + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new ChannelConnectorException("Channex ack transport error: " + e.getMessage(), e);
        }
    }

    // Defensive parsing — treat every field as possibly missing.
    private BookingRevision parseRevision(JsonNode item) {
        JsonNode a = item.path("attributes");
        JsonNode cust = a.path("customer");
        String fullName = (text(cust, "name") + " " + text(cust, "surname")).trim();

        List<BookingRevision.RoomSegment> rooms = new ArrayList<>();
        for (JsonNode r : a.path("rooms")) {
            JsonNode occ = r.path("occupancy");
            rooms.add(new BookingRevision.RoomSegment(
                    text(r, "room_type_id"),
                    text(r, "rate_plan_id"),
                    date(r, "checkin_date"),
                    date(r, "checkout_date"),
                    occ.path("adults").asInt(1),
                    occ.path("children").asInt(0)));
        }

        return new BookingRevision(
                text(item, "id"),
                text(a, "booking_id"),
                parseStatus(text(a, "status")),
                text(a, "property_id"),
                text(a, "ota_name"),
                text(a, "ota_reservation_code"),
                fullName.isBlank() ? null : fullName,
                text(cust, "mail"),
                text(cust, "phone"),
                text(cust, "country"),
                rooms);
    }

    private static RevisionStatus parseStatus(String s) {
        if (s == null) return RevisionStatus.UNKNOWN;
        return switch (s.toLowerCase()) {
            case "new" -> RevisionStatus.NEW;
            case "modified", "modification" -> RevisionStatus.MODIFIED;
            case "cancelled", "cancellation" -> RevisionStatus.CANCELLED;
            default -> RevisionStatus.UNKNOWN;
        };
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return v.isMissingNode() || v.isNull() ? null : v.asText();
    }

    private static LocalDate date(JsonNode node, String field) {
        String v = text(node, field);
        return (v == null || v.isBlank()) ? null : LocalDate.parse(v);
    }
}
