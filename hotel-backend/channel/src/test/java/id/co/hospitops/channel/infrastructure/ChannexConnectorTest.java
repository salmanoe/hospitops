package id.co.hospitops.channel.infrastructure;

import id.co.hospitops.channel.domain.model.AriUpdate;
import id.co.hospitops.channel.domain.port.out.ChannelConnectorException;
import id.co.hospitops.channel.infrastructure.config.ChannexProperties;
import id.co.hospitops.channel.infrastructure.connector.ChannexConnector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("ChannexConnector")
class ChannexConnectorTest {

    private static ChannexProperties props(boolean enabled, String apiKey) {
        ChannexProperties p = new ChannexProperties();
        p.setBaseUrl("https://staging.channex.io/api/v1");
        p.setEnabled(enabled);
        p.setApiKey(apiKey);
        return p;
    }

    private static final List<AriUpdate> ARI = List.of(
            new AriUpdate("rt-1", "rp-1", LocalDate.of(2026, 6, 13), 4, new BigDecimal("500000")));

    @Test
    @DisplayName("throws when disabled or missing API key")
    void throwsWhenDisabled() {
        ChannexConnector connector = new ChannexConnector(RestClient.builder().build(), props(false, ""));
        assertThatThrownBy(() -> connector.pushAri("prop-1", ARI))
                .isInstanceOf(ChannelConnectorException.class);
    }

    @Test
    @DisplayName("POSTs availability then restrictions to Channex when enabled")
    void postsAriToChannex() {
        ChannexProperties props = props(true, "test-key");
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(props.getBaseUrl())
                .defaultHeader("user-api-key", props.getApiKey());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        server.expect(requestTo(endsWith("/availability")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("user-api-key", "test-key"))
                .andExpect(jsonPath("$.values[0].room_type_id").value("rt-1"))
                .andRespond(withSuccess());
        server.expect(requestTo(endsWith("/restrictions")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.values[0].rate_plan_id").value("rp-1"))
                .andRespond(withSuccess());

        ChannexConnector connector = new ChannexConnector(builder.build(), props);
        connector.pushAri("prop-1", ARI);

        server.verify();
    }
}
