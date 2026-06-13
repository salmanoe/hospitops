package id.co.hospitops.channel.domain.model;

import id.co.hospitops.shared.ChannelSyncMessageId;
import id.co.hospitops.shared.HotelId;
import lombok.Getter;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Transactional-outbox entry. The hotel-scoped write that triggers a sync
 * (e.g. a reservation change) persists one of these in the same transaction;
 * a background relay later delivers it to the provider with at-least-once
 * semantics. Provider ARI updates are idempotent, so re-delivery is safe.
 */
@Getter
public class ChannelSyncMessage {

    /** Give up (dead-letter) after this many failed attempts. */
    public static final int MAX_ATTEMPTS = 6;

    private final ChannelSyncMessageId id;
    private final HotelId hotelId;
    private final SyncMessageType type;
    private final String payload;
    private SyncStatus status;
    private int attempts;
    private String lastError;
    private LocalDateTime nextAttemptAt;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ChannelSyncMessage create(HotelId hotelId, SyncMessageType type, String payload) {
        LocalDateTime now = LocalDateTime.now();
        return new ChannelSyncMessage(ChannelSyncMessageId.generate(), hotelId, type, payload,
                SyncStatus.PENDING, 0, null, now, now, now);
    }

    public static ChannelSyncMessage reconstitute(ChannelSyncMessageId id, HotelId hotelId,
                                                  SyncMessageType type, String payload, SyncStatus status,
                                                  int attempts, String lastError, LocalDateTime nextAttemptAt,
                                                  LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new ChannelSyncMessage(id, hotelId, type, payload, status, attempts, lastError,
                nextAttemptAt, createdAt, updatedAt);
    }

    private ChannelSyncMessage(ChannelSyncMessageId id, HotelId hotelId, SyncMessageType type, String payload,
                               SyncStatus status, int attempts, String lastError, LocalDateTime nextAttemptAt,
                               LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.hotelId = hotelId;
        this.type = type;
        this.payload = payload;
        this.status = status;
        this.attempts = attempts;
        this.lastError = lastError;
        this.nextAttemptAt = nextAttemptAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** Mark a successful delivery. */
    public void markSent() {
        this.status = SyncStatus.SENT;
        this.lastError = null;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Record a failed delivery attempt. Schedules an exponential backoff retry
     * (capped at 1 hour) until {@link #MAX_ATTEMPTS} is reached, after which the
     * message is dead-lettered as {@link SyncStatus#FAILED}.
     */
    public void markFailed(String error) {
        this.attempts++;
        this.lastError = error;
        this.updatedAt = LocalDateTime.now();
        if (this.attempts >= MAX_ATTEMPTS) {
            this.status = SyncStatus.FAILED;
        } else {
            this.status = SyncStatus.PENDING;
            long backoffSeconds = Math.min(60L * (1L << (this.attempts - 1)), 3600L);
            this.nextAttemptAt = LocalDateTime.now().plus(Duration.ofSeconds(backoffSeconds));
        }
    }
}
