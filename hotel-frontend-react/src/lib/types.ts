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
  roomTypeName?: string;
  capacity?: number;
  basePrice?: number;
  notes?: string;
  /** Date-range price returned by GET /rooms/available. */
  effectiveRate?: number;
}

export interface RoomType {
  id: string;
  name: string;
  capacity: number;
  basePrice: number;
  description?: string;
}

export interface Staff {
  id: string;
  fullName: string;
  username: string;
  role: Role;
  active: boolean;
  createdAt?: string;
}

export type SetupStep = "PROFILE" | "POLICY" | "ROOM_TYPE" | "ROOM" | "STAFF_ACCOUNT";

export interface HkRoom {
  roomId: string;
  roomNumber: string;
  roomTypeName?: string;
  status: RoomStatus;
}
export interface HousekeepingFloor {
  floor: string;
  rooms: HkRoom[];
}

export type HotelStatus = "SETUP" | "ACTIVE" | "SUSPENDED";

export interface Hotel {
  id: string;
  name: string;
  status: HotelStatus;
  timezone?: string;
  currency?: string;
  starRating?: number;
  remainingSetupSteps?: SetupStep[];
}

export interface PolicyConfig {
  taxPercent: number;
  taxName: string;
  invoiceHotelName: string;
  invoiceAddress?: string | null;
  invoiceFooterNote?: string | null;
}

export interface GroupProfile {
  id: string;
  name: string;
  ownerEmail: string;
  createdAt?: string;
}

export interface HotelSummary {
  hotelId: string;
  hotelName?: string;
  hotelStatus: HotelStatus;
  occupiedRooms: number;
  totalRooms: number;
  arrivalsToday: number;
  departuresToday: number;
  revenueToday: number;
  revenueMonth: number;
  dirtyRooms: number;
}

export type PaymentMethod = "CASH" | "CREDIT_CARD" | "DEBIT_CARD" | "BANK_TRANSFER";
export type PaymentStatus = "UNPAID" | "PARTIAL" | "PAID";

export interface InvoiceItem {
  description: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
}

export interface Payment {
  amount: number;
  method: PaymentMethod;
  referenceNo?: string | null;
  paidAt?: string;
}

export interface Invoice {
  id: string;
  invoiceNumber: string;
  reservationNumber?: string;
  guestName?: string;
  totalAmount: number;
  totalPaid: number;
  balance: number;
  paymentStatus: PaymentStatus;
  issuedAt?: string;
  // Detail-only fields (populated by GET /invoices/:id):
  reservationId?: string;
  subtotal?: number;
  taxAmount?: number;
  discountAmount?: number;
  dueDate?: string | null;
  notes?: string | null;
  items?: InvoiceItem[];
  payments?: Payment[];
}

export type ReservationStatus =
  | "PENDING"
  | "CONFIRMED"
  | "CHECKED_IN"
  | "CHECKED_OUT"
  | "CANCELLED";

/** Reservation — covers dashboard arrivals/departures, the list, and the detail view. */
export interface ReservationSummary {
  id: string;
  reservationNumber?: string;
  guestId: string;
  guestFullName?: string;
  roomId: string;
  roomNumber?: string;
  checkInDate?: string;
  checkOutDate?: string;
  nights: number;
  status: ReservationStatus;
  // Detail-only fields:
  adults?: number;
  children?: number;
  ratePerNight?: number;
  subtotal?: number;
  specialRequests?: string;
  createdAt?: string;
}

/** Payload for creating a reservation (POST /reservations). */
export interface CreateReservationInput {
  guestId: string;
  roomId: string;
  checkIn: string;
  checkOut: string;
  adults: number;
  children: number;
  specialRequests?: string | null;
}

export interface Guest {
  id: string;
  fullName: string;
  idNumber?: string;
  nationality?: string;
  phone?: string;
  email?: string;
  address?: string;
  createdAt?: string;
}

// ── Rate / availability calendar ───────────────────────────────────────────
export interface RoomCalendarDay {
  date: string;
  available: number;
  rate: number;
}

export interface RoomCalendarRow {
  roomTypeId: string;
  name: string;
  capacity: number;
  days: RoomCalendarDay[];
}

// ── Channel manager (Channex) ──────────────────────────────────────────────
export interface ChannelProperty {
  id: string;
  provider: string;
  externalPropertyId: string;
  enabled: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface ChannelRoomTypeMapping {
  id: string;
  roomTypeId: string;
  externalRoomTypeId: string;
  externalRatePlanId: string;
  createdAt?: string;
  updatedAt?: string;
}

export type SyncStatus = "PENDING" | "SENT" | "FAILED";

export interface ChannelSyncMessage {
  id: string;
  type: string;
  status: SyncStatus;
  attempts: number;
  lastError?: string | null;
  nextAttemptAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

export type InboundStatus = "BOOKED" | "CANCELLED" | "CONFLICT";

export interface ChannelInboundBooking {
  externalBookingId: string;
  status: InboundStatus;
  otaName?: string | null;
  otaReservationCode?: string | null;
  reservationId?: string | null;
  lastRevisionId?: string | null;
  createdAt?: string;
  updatedAt?: string;
}
