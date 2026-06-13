package id.co.hospitops.channel.infrastructure.persistence;

import id.co.hospitops.channel.domain.model.ChannelSyncMessage;
import id.co.hospitops.channel.domain.model.SyncStatus;
import id.co.hospitops.channel.domain.port.out.ChannelSyncMessageRepository;
import id.co.hospitops.channel.infrastructure.persistence.entity.ChannelSyncMessageJpaEntity;
import id.co.hospitops.shared.ChannelSyncMessageId;
import id.co.hospitops.shared.HotelContext;
import id.co.hospitops.shared.HotelId;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ChannelSyncMessageRepositoryImpl implements ChannelSyncMessageRepository {

    private final ChannelSyncMessageJpaRepository jpa;

    @Override
    public ChannelSyncMessage save(ChannelSyncMessage message) {
        // Copy onto the managed entity when present (status/attempt updates from
        // the relay); insert otherwise. Not hotel-scoped — the relay runs globally.
        ChannelSyncMessageJpaEntity entity = jpa.findById(message.getId().value())
                .map(existing -> {
                    existing.setStatus(message.getStatus());
                    existing.setAttempts(message.getAttempts());
                    existing.setLastError(message.getLastError());
                    existing.setNextAttemptAt(message.getNextAttemptAt());
                    return existing;
                })
                .orElseGet(() -> toJpa(message));
        return toDomain(jpa.save(entity));
    }

    @Override
    public List<ChannelSyncMessage> findProcessable(LocalDateTime now, int limit) {
        return jpa.findByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                        SyncStatus.PENDING, now, PageRequest.of(0, limit))
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<ChannelSyncMessage> findRecentForCurrentHotel(int limit) {
        return jpa.findByHotelIdOrderByCreatedAtDesc(
                        HotelContext.current().value(), PageRequest.of(0, limit))
                .stream().map(this::toDomain).toList();
    }

    // ── Mapping ─────────────────────────────────────────────────────────

    private ChannelSyncMessageJpaEntity toJpa(ChannelSyncMessage m) {
        return ChannelSyncMessageJpaEntity.builder()
                .id(m.getId().value())
                .hotelId(m.getHotelId().value())
                .type(m.getType())
                .payload(m.getPayload())
                .status(m.getStatus())
                .attempts(m.getAttempts())
                .lastError(m.getLastError())
                .nextAttemptAt(m.getNextAttemptAt())
                .build();
    }

    private ChannelSyncMessage toDomain(ChannelSyncMessageJpaEntity e) {
        return ChannelSyncMessage.reconstitute(
                ChannelSyncMessageId.of(e.getId()),
                HotelId.of(e.getHotelId()),
                e.getType(),
                e.getPayload(),
                e.getStatus(),
                e.getAttempts(),
                e.getLastError(),
                e.getNextAttemptAt(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }
}
