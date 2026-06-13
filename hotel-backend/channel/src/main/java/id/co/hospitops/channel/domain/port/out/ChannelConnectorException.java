package id.co.hospitops.channel.domain.port.out;

/** Raised by a {@link ChannelConnectorPort} when a push cannot be delivered. */
public class ChannelConnectorException extends RuntimeException {
    public ChannelConnectorException(String message) {
        super(message);
    }

    public ChannelConnectorException(String message, Throwable cause) {
        super(message, cause);
    }
}
