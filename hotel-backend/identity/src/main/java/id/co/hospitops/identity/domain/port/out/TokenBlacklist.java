package id.co.hospitops.identity.domain.port.out;

public interface TokenBlacklist {
    void invalidate(String token);

    boolean isBlacklisted(String token);
}
