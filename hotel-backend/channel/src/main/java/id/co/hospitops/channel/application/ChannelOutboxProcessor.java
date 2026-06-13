package id.co.hospitops.channel.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.co.hospitops.channel.application.payload.ChannelSyncPayload;
import id.co.hospitops.channel.domain.model.ChannelSyncMessage;
import id.co.hospitops.channel.domain.port.out.ChannelConnectorPort;
import id.co.hospitops.channel.domain.port.out.ChannelSyncMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Drains the outbox: picks up due PENDING messages (across all hotels) and
 * delivers them to the provider, marking each SENT or scheduling a backoff
 * retry. Runs with no {@code HotelContext} bound — the payload is self-contained
 * (it carries the provider property id), so no tenant scope is required.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelOutboxProcessor {

    private static final int BATCH_SIZE = 50;

    private final ChannelSyncMessageRepository syncRepo;
    private final ChannelConnectorPort connector;
    private final ObjectMapper objectMapper;

    /**
     * Process one batch of due messages. Each message is handled independently;
     * a delivery failure schedules a retry but does not abort the batch.
     *
     * @return the number of messages processed
     */
    @Transactional
    public int processBatch() {
        List<ChannelSyncMessage> due = syncRepo.findProcessable(LocalDateTime.now(), BATCH_SIZE);
        for (ChannelSyncMessage message : due) {
            deliver(message);
            syncRepo.save(message);
        }
        return due.size();
    }

    private void deliver(ChannelSyncMessage message) {
        try {
            ChannelSyncPayload payload = objectMapper.readValue(message.getPayload(), ChannelSyncPayload.class);
            connector.pushAri(payload.propertyId(), payload.updates());
            message.markSent();
        } catch (Exception e) {
            log.warn("Channel sync message {} delivery failed (attempt {}): {}",
                    message.getId().value(), message.getAttempts() + 1, e.getMessage());
            message.markFailed(truncate(e.getMessage()));
        }
    }

    private static String truncate(String s) {
        if (s == null) return null;
        return s.length() <= 1000 ? s : s.substring(0, 1000);
    }
}
