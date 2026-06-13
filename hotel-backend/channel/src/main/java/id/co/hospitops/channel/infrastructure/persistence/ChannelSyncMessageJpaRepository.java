package id.co.hospitops.channel.infrastructure.persistence;

import id.co.hospitops.channel.domain.model.SyncStatus;
import id.co.hospitops.channel.infrastructure.persistence.entity.ChannelSyncMessageJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ChannelSyncMessageJpaRepository
        extends JpaRepository<ChannelSyncMessageJpaEntity, UUID> {

    /** Due messages across all hotels, oldest first (relay is not hotel-scoped). */
    List<ChannelSyncMessageJpaEntity> findByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            SyncStatus status, LocalDateTime now, Pageable pageable);

    /** Recent messages for one hotel, newest first (sync-status board). */
    List<ChannelSyncMessageJpaEntity> findByHotelIdOrderByCreatedAtDesc(UUID hotelId, Pageable pageable);
}
