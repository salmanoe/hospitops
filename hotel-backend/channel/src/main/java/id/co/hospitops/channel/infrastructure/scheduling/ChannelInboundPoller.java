package id.co.hospitops.channel.infrastructure.scheduling;

import id.co.hospitops.channel.application.ChannelInboundService;
import id.co.hospitops.channel.domain.model.BookingRevision;
import id.co.hospitops.channel.domain.port.out.ChannelConnectorPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Polls the provider's booking-revisions feed and applies each revision.
 *
 * <p>Processes ONE feed page per tick: a revision is acked only after it is
 * applied successfully, so a failing ("poison") revision is retried on later
 * ticks instead of spinning a drain loop. Acked revisions drop from the feed,
 * so subsequent ticks advance. Only created when {@code channex.inbound.enabled=true}.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "channex.inbound", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class ChannelInboundPoller {

    private final ChannelConnectorPort connector;
    private final ChannelInboundService inboundService;

    @Scheduled(fixedDelayString = "${channex.inbound.poll-ms:20000}")
    public void poll() {
        List<BookingRevision> batch;
        try {
            batch = connector.fetchRevisionFeed();
        } catch (Exception e) {
            log.error("Channel inbound feed fetch failed", e);
            return;
        }
        if (batch.isEmpty()) return;

        int applied = 0;
        for (BookingRevision rev : batch) {
            try {
                inboundService.process(rev);
                connector.ackRevision(rev.revisionId());   // ack only on success
                applied++;
            } catch (Exception e) {
                // Leave it un-acked → re-served within the 30-minute window.
                log.error("Inbound revision {} failed; will retry next cycle: {}",
                        rev.revisionId(), e.getMessage());
            }
        }
        log.info("Channel inbound: applied {}/{} revision(s)", applied, batch.size());
    }
}
