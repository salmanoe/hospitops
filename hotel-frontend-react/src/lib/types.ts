// Domain types mirroring the backend DTOs (id.co.hospitops.*.application.response)
// and the ApiResponse / PageResult envelopes from the shared module.

export type Role =
  | "GROUP_ADMIN"
  | "ADMIN"
  | "MANAGER"
  | "FRONT_DESK"
  | "ACCOUNTANT"
  | "HOUSEKEEPING";

/** The shared ApiResponse<T> wrapper: { data, message, error, timestamp }. */
export interface ApiEnvelope<T> {
  data?: T;
  message?: string;
  error?: unknown;
}

/** The shared PageResult<T> shape returned by paginated endpoints. */
export interface PageResult<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/** Normalised session user (both staff and GROUP_ADMIN logins map to this). */
export interface StoredUser {
  id: string;
  name: string;
  username: string;
  role: Role;
  groupId: string | null;
  hotelId: string | null;
  hotelName?: string | null;
}

/** POST /api/v1/hotels/{id}/auth/login → LoginResponse */
export interface StaffLoginResponse {
  token: string;
  refreshToken?: string;
  staffId: string;
  fullName: string;
  username: string;
  role: Role;
}

/** POST /api/v1/group/auth/login and /enter → GroupLoginResponse */
export interface GroupLoginResponse {
  accessToken: string;
  refreshToken?: string;
  adminId: string;
  groupId?: string;
  email: string;
  role: "GROUP_ADMIN";
  hotelId?: string | null;
  hotelName?: string | null;
}

/**
 * Either login shape, flattened. (Intersecting the two response types collapses
 * `role` to the GROUP_ADMIN literal, so the fields are listed explicitly here.)
 */
export interface AnyLoginResponse {
  token?: string;
  accessToken?: string;
  refreshToken?: string;
  staffId?: string;
  fullName?: string;
  username?: string;
  adminId?: string;
  groupId?: string;
  email?: string;
  hotelId?: string | null;
  hotelName?: string | null;
  role: Role;
}

export type RoomStatus =
  | "AVAILABLE"
  | "OCCUPIED"
  | "DIRTY"
  | "MAINTENANCE"
  | "SERVICE_REQUESTED";

export interface Room {
  id: string;
  roomNumber: string;
  status: RoomStatus;
  floor?: string;
  roomTypeId?: string;
}

export type ReservationStatus =
  | "PENDING"
  | "CONFIRMED"
  | "CHECKED_IN"
  | "CHECKED_OUT"
  | "CANCELLED";

/** Reservation summary used by the dashboard arrivals/departures lists. */
export interface ReservationSummary {
  id: string;
  guestId: string;
  guestFullName?: string;
  roomId: string;
  roomNumber?: string;
  nights: number;
  status: ReservationStatus;
}
