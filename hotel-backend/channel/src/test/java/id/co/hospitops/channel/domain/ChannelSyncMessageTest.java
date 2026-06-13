package id.co.hospitops.channel.domain;

import id.co.hospitops.channel.domain.model.ChannelSyncMessage;
import id.co.hospitops.channel.domain.model.SyncMessageType;
import id.co.hospitops.channel.domain.model.SyncStatus;
import id.co.hospitops.shared.HotelId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ChannelSyncMessage")
class ChannelSyncMessageTest {

    private static ChannelSyncMessage newMessage() {
        return ChannelSyncMessage.create(HotelId.generate(), SyncMessageType.ARI_PUSH, "{}");
    }

    @Test
    @DisplayName("starts PENDING with zero attempts")
    void startsPending() {
        ChannelSyncMessage m = newMessage();
        assertThat(m.getStatus()).isEqualTo(SyncStatus.PENDING);
        assertThat(m.getAttempts()).isZero();
    }

    @Test
    @DisplayName("markSent() marks it SENT and clears the error")
    void markSent() {
        ChannelSyncMessage m = newMessage();
        m.markFailed("earlier error");
        m.markSent();
        assertThat(m.getStatus()).isEqualTo(SyncStatus.SENT);
        assertThat(m.getLastError()).isNull();
    }

    @Test
    @DisplayName("markFailed() increments attempts and schedules a future retry while under the cap")
    void markFailedSchedulesRetry() {
        ChannelSyncMessage m = newMessage();
        m.markFailed("boom");
        assertThat(m.getAttempts()).isEqualTo(1);
        assertThat(m.getStatus()).isEqualTo(SyncStatus.PENDING);
        assertThat(m.getLastError()).isEqualTo("boom");
        assertThat(m.getNextAttemptAt()).isAfter(LocalDateTime.now());
    }

    @Test
    @DisplayName("dead-letters to FAILED after MAX_ATTEMPTS")
    void deadLettersAfterMaxAttempts() {
        ChannelSyncMessage m = newMessage();
        for (int i = 0; i < ChannelSyncMessage.MAX_ATTEMPTS; i++) {
            m.markFailed("boom " + i);
        }
        assertThat(m.getAttempts()).isEqualTo(ChannelSyncMessage.MAX_ATTEMPTS);
        assertThat(m.getStatus()).isEqualTo(SyncStatus.FAILED);
    }
}
