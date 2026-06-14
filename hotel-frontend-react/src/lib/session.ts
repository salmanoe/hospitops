// Low-level session storage — same localStorage keys as the legacy core.js so
// the React app is drop-in compatible with an existing logged-in session.
import type { AnyLoginResponse, StoredUser } from "./types";

const TOKEN_KEY = "hospitops_token";
const USER_KEY = "hospitops_user";
const REFRESH_TOKEN_KEY = "hospitops_refresh_token";
const RECENT_HOTELS_KEY = "hospitops_recent_hotels";

const MAX_RECENT_HOTELS = 6;

/** A hotel the staff member has signed into before, remembered on this device. */
export interface RecentHotel {
  id: string;
  name: string;
}

export const getRecentHotels = (): RecentHotel[] => {
  try {
    const raw = localStorage.getItem(RECENT_HOTELS_KEY);
    const list = raw ? (JSON.parse(raw) as RecentHotel[]) : [];
    return Array.isArray(list) ? list.filter((h) => h && h.id) : [];
  } catch {
    return [];
  }
};

/** Add or refresh a hotel in the recent list (most-recent first, deduped by id). */
export const addRecentHotel = (hotel: RecentHotel): void => {
  const id = hotel.id.trim();
  if (!id) return;
  const name = hotel.name.trim() || id;
  const next = [
    { id, name },
    ...getRecentHotels().filter((h) => h.id !== id),
  ].slice(0, MAX_RECENT_HOTELS);
  localStorage.setItem(RECENT_HOTELS_KEY, JSON.stringify(next));
};

export const removeRecentHotel = (id: string): void => {
  localStorage.setItem(
    RECENT_HOTELS_KEY,
    JSON.stringify(getRecentHotels().filter((h) => h.id !== id)),
  );
};

export const getToken = () => localStorage.getItem(TOKEN_KEY);
export const getRefreshToken = () => localStorage.getItem(REFRESH_TOKEN_KEY);

export const getUser = (): StoredUser | null => {
  const raw = localStorage.getItem(USER_KEY);
  return raw ? (JSON.parse(raw) as StoredUser) : null;
};

export const clearSession = () => {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
};

/**
 * Persist a login response from either the staff endpoint (token, staffId,
 * fullName, username, role) or the GROUP_ADMIN endpoints (accessToken, adminId,
 * groupId, email, role='GROUP_ADMIN', hotelId). Both normalise to StoredUser.
 */
export const saveSession = (res: AnyLoginResponse): StoredUser => {
  const token = res.accessToken || res.token;
  if (token) localStorage.setItem(TOKEN_KEY, token);
  if (res.refreshToken) localStorage.setItem(REFRESH_TOKEN_KEY, res.refreshToken);

  const isGroupAdmin = res.role === "GROUP_ADMIN";
  const user: StoredUser = isGroupAdmin
    ? {
        id: res.adminId ?? "",
        name: res.email ?? "",
        username: res.email ?? "",
        role: res.role,
        groupId: res.groupId ?? null,
        hotelId: res.hotelId ?? null,
        hotelName: res.hotelName ?? null,
      }
    : {
        id: res.staffId ?? "",
        name: res.fullName ?? "",
        username: res.username ?? "",
        role: res.role,
        groupId: null,
        hotelId: null,
      };

  localStorage.setItem(USER_KEY, JSON.stringify(user));
  return user;
};

export const isGroupAdmin = (u: StoredUser | null) => u?.role === "GROUP_ADMIN";
export const isHotelScoped = (u: StoredUser | null) =>
  u?.role === "GROUP_ADMIN" && !!u.hotelId;
