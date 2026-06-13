package id.co.hospitops.channel.application;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import id.co.hospitops.channel.application.payload.ChannelSyncPayload;
import id.co.hospitops.channel.domain.model.AriUpdate;
import id.co.hospitops.channel.domain.model.ChannelSyncMessage;
import id.co.hospitops.channel.domain.model.SyncMessageType;
import id.co.hospitops.channel.domain.model.SyncStatus;
import id.co.hospitops.channel.domain.port.out.ChannelConnectorException;
import id.co.hospitops.channel.domain.port.out.ChannelConnectorPort;
import id.co.hospitops.channel.domain.port.out.ChannelSyncMessageRepository;
import id.co.hospitops.shared.HotelId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("ChannelOutboxProcessor")
@ExtendWith(MockitoExtension.class)
class ChannelOutboxProcessorTest {

    @Mock
    ChannelSyncMessageRepository syncRepo;
    @Mock
    ChannelConnectorPort connector;

    final ObjectMapper objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();

    ChannelOutboxProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new ChannelOutboxProcessor(syncRepo, connector, objectMapper);
    }

    private ChannelSyncMessage ariMessage() throws Exception {
        ChannelSyncPayload payload = new ChannelSyncPayload("prop-1",
                List.of(new AriUpdate("rt-1", "rp-1", LocalDate.now(), 3, new BigDecimal("500000"))));
        return ChannelSyncMessage.create(HotelId.generate(), SyncMessageType.ARI_PUSH,
                objectMapper.writeValueAsString(payload));
    }

    @Test
    @DisplayName("delivers a due message and marks it SENT")
    void deliversAndMarksSent() throws Exception {
        ChannelSyncMessage msg = ariMessage();
        when(syncRepo.findProcessable(any(), anyInt())).thenReturn(List.of(msg));

        int processed = processor.processBatch();

        assertThat(processed).isEqualTo(1);
        verify(connector).pushAri(eq("prop-1"), anyList());
        assertThat(msg.getStatus()).isEqualTo(SyncStatus.SENT);
        verify(syncRepo).save(msg);
    }

    @Test
    @DisplayName("schedules a retry when the connector fails")
    void retriesOnFailure() throws Exception {
        ChannelSyncMessage msg = ariMessage();
        when(syncRepo.findProcessable(any(), anyInt())).thenReturn(List.of(msg));
        doThrow(new ChannelConnectorException("provider 500")).when(connector).pushAri(any(), anyList());

        processor.processBatch();

        assertThat(msg.getStatus()).isEqualTo(SyncStatus.PENDING);
        assertThat(msg.getAttempts()).isEqualTo(1);
        assertThat(msg.getLastError()).contains("provider 500");
        verify(syncRepo).save(msg);
    }
}
