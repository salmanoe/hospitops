package id.co.hospitops.channel.application;

import id.co.hospitops.channel.application.response.ChannelInboundBookingResponse;
import id.co.hospitops.channel.application.response.ChannelSyncMessageResponse;
import id.co.hospitops.channel.domain.port.in.ChannelStatusUseCase;
import id.co.hospitops.channel.domain.port.out.ChannelInboundBookingRepository;
import id.co.hospitops.channel.domain.port.out.ChannelSyncMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ChannelStatusService implements ChannelStatusUseCase {

    private static final int MAX_LIMIT = 100;

    private final ChannelSyncMessageRepository syncRepo;
    private final ChannelInboundBookingRepository inboundRepo;

    @Override
    public List<ChannelSyncMessageResponse> recentSyncMessages(int limit) {
        return syncRepo.findRecentForCurrentHotel(clamp(limit)).stream()
                .map(ChannelSyncMessageResponse::from).toList();
    }

    @Override
    public List<ChannelInboundBookingResponse> recentInboundBookings(int limit) {
        return inboundRepo.findRecentForCurrentHotel(clamp(limit)).stream()
                .map(ChannelInboundBookingResponse::from).toList();
    }

    private static int clamp(int limit) {
        return Math.min(Math.max(limit, 1), MAX_LIMIT);
    }
}
