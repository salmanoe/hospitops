package id.co.hospitops.bootstrap;

import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import io.lettuce.core.resource.MappingSocketAddressResolver;
import java.net.InetAddress;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Lettuce client configuration for containerised deployments.
 *
 * <p>Only active when {@code hospitops.redis.enabled=true}.
 */
@Configuration
@ConditionalOnProperty(name = "hospitops.redis.enabled", havingValue = "true")
public class RedisConfig {

    /**
     * Forces Lettuce to use the JVM's built-in DNS resolver ({@code InetAddress})
     * instead of Netty's async DNS pipeline.
     *
     * <p><strong>Why this is needed in Docker Compose:</strong><br>
     * Netty builds its own async DNS resolver that reads nameservers directly from
     * {@code /etc/resolv.conf}. In Docker containers this file points to Docker's
     * embedded DNS at {@code 127.0.0.11}, which resolves Compose service names
     * (e.g. {@code redis}). However, Netty's resolver occasionally fails to
     * honour this in certain container network configurations, producing:
     * <pre>java.net.UnknownHostException: Failed to resolve 'redis' [A(1)]</pre>
     *
     * <p>{@code MappingSocketAddressResolver} with {@code InetAddress::getAllByName}
     * delegates every lookup to the JVM's OS resolver, which always consults
     * Docker's internal hostnames correctly. This is the non-deprecated replacement
     * for the retired {@code DnsResolvers.JVM_DEFAULT} constant.
     *
     * <p>Spring Boot's Lettuce auto-configuration is
     * {@code @ConditionalOnMissingBean(ClientResources.class)}, so registering
     * this bean causes the auto-configured default to back off automatically.
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean(ClientResources.class)
    public ClientResources lettuceClientResources() {
        return DefaultClientResources.builder()
                .socketAddressResolver(MappingSocketAddressResolver.create(InetAddress::getAllByName))
                .build();
    }
}
