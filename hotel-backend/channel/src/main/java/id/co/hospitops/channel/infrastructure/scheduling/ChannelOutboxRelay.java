package id.co.hospitops.channel.infrastructure.scheduling;

import id.co.hospitops.channel.application.ChannelOutboxProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically drains the channel outbox. Only created when
 * {@code channex.relay.enabled=true} — kept off until a Channex API key is
 * configured, so the app never attempts deliveries with no credentials.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "channex.relay", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class ChannelOutboxRelay {

    private final ChannelOutboxProcessor processor;

    @Scheduled(fixedDelayString = "${channex.relay.poll-ms:15000}")
    public void poll() {
        try {
            int processed = processor.processBatch();
            if (processed > 0) {
                log.debug("Channel outbox relay processed {} message(s)", processed);
            }
        } catch (Exception e) {
            // Never let a relay tick die — the next tick retries.
            log.error("Channel outbox relay tick failed", e);
        }
    }
}
