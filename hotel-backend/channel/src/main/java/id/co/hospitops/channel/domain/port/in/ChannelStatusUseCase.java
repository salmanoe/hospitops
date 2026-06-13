package id.co.hospitops.channel.domain.port.in;

import id.co.hospitops.channel.application.response.ChannelInboundBookingResponse;
import id.co.hospitops.channel.application.response.ChannelSyncMessageResponse;

import java.util.List;

/** Read model behind the sync-status board for the current hotel. */
public interface ChannelStatusUseCase {

    List<ChannelSyncMessageResponse> recentSyncMessages(int limit);

    List<ChannelInboundBookingResponse> recentInboundBookings(int limit);
}
