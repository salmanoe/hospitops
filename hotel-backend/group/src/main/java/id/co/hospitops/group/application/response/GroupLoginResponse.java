package id.co.hospitops.group.application.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import id.co.hospitops.shared.GroupAdminId;
import id.co.hospitops.shared.GroupId;
import id.co.hospitops.shared.HotelId;
import org.jspecify.annotations.Nullable;

/**
 * Response for GROUP_ADMIN login and hotel token-exchange endpoints.
 *
 * <p>{@code hotelId} is non-null only for hotel-scoped tokens issued by {@code /enter}.
 * Null fields are omitted from JSON serialization so clients can use field presence
 * to distinguish group-scoped from hotel-scoped tokens.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GroupLoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String role,
        GroupAdminId adminId,
        GroupId groupId,
        String email,
        @Nullable HotelId hotelId,
        @Nullable String hotelName
) {
    public static GroupLoginResponse groupScoped(String accessToken, long expiresIn,
                                                 GroupAdminId adminId, GroupId groupId,
                                                 String email) {
        return new GroupLoginResponse(accessToken, "Bearer", expiresIn,
                "GROUP_ADMIN", adminId, groupId, email, null, null);
    }

    public static GroupLoginResponse hotelScoped(String accessToken, long expiresIn,
                                                 GroupAdminId adminId, GroupId groupId,
                                                 String email, HotelId hotelId,
                                                 String hotelName) {
        return new GroupLoginResponse(accessToken, "Bearer", expiresIn,
                "GROUP_ADMIN", adminId, groupId, email, hotelId, hotelName);
    }
}
