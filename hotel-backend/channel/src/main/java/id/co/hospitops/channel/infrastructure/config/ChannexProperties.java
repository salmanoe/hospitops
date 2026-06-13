package id.co.hospitops.channel.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Channex connection settings. Bound from the {@code channex.*} properties
 * (see application.yml), which in turn read CHANNEX_* environment variables.
 *
 * <p>Defaults point at the free staging sandbox and keep the connector + relay
 * OFF until an API key is supplied, so the module is inert until configured.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "channex")
public class ChannexProperties {

    /** Sandbox by default; set to the production base URL when going live. */
    private String baseUrl = "https://staging.channex.io/api/v1";

    /** Channex API key (header {@code user-api-key}). Empty = connector disabled. */
    private String apiKey = "";

    /** Master switch — when false the connector refuses to send. */
    private boolean enabled = false;

    private Relay relay = new Relay();

    @Getter
    @Setter
    public static class Relay {
        /** When false the scheduled outbox relay bean is not created. */
        private boolean enabled = false;
        /** Poll interval for the outbox relay, milliseconds. */
        private long pollMs = 15000;
    }
}
