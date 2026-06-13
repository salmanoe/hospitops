package id.co.hospitops.channel.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Wires the Channex HTTP client. The API key is sent on every request via the
 * {@code user-api-key} header that Channex expects.
 */
@Configuration
@EnableConfigurationProperties(ChannexProperties.class)
public class ChannelConfig {

    @Bean
    RestClient channexRestClient(ChannexProperties props) {
        return RestClient.builder()
                .baseUrl(props.getBaseUrl())
                .defaultHeader("user-api-key", props.getApiKey())
                .build();
    }
}
