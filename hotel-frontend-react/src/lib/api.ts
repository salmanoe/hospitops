// Centralised API client — mirrors the legacy core.js API module:
//   • injects the Bearer token
//   • unwraps the ApiResponse envelope (returns body.data)
//   • on 401, attempts ONE silent refresh + retry, then clears the session
import axios, {
  AxiosError,
  type AxiosInstance,
  type AxiosRequestConfig,
  type InternalAxiosRequestConfig,
} from "axios";
import { clearSession, getRefreshToken, getToken, saveSession } from "./session";
import type {
  AnyLoginResponse,
  ApiEnvelope,
  CreateReservationInput,
  Guest,
  GroupLoginResponse,
  HotelSummary,
  HousekeepingFloor,
  Invoice,
  PageResult,
  PaymentMethod,
  ReservationSummary,
  Room,
  RoomStatus,
  RoomType,
  SetupStep,
  Staff,
  StaffLoginResponse,
} from "./types";

export interface PaymentInput {
  amount: number;
  method: PaymentMethod;
  referenceNo?: string | null;
}

export class ApiError extends Error {
  status: number;
  details: unknown;
  constructor(status: number, message: string, details?: unknown) {
    super(message);
    this.status = status;
    this.details = details;
  }
}

const BASE = "/api/v1";

const http: AxiosInstance = axios.create({ baseURL: BASE });

http.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getToken();
  config.headers.set("Content-Type", "application/json");
  config.headers.set("Authorization", `Bearer ${token ?? ""}`);
  return config;
});

// ── Silent token refresh — one shared promise for concurrent 401s ──────────
let refreshPromise: Promise<boolean> | null = null;

const refreshSession = (): Promise<boolean> => {
  if (refreshPromise) return refreshPromise;
  refreshPromise = (async () => {
    try {
      const refreshToken = getRefreshToken();
      if (!refreshToken) return false;
      const res = await axios.post<ApiEnvelope<AnyLoginResponse>>(
        `${BASE}/auth/refresh`,
        { refreshToken },
        { headers: { "Content-Type": "application/json" } },
      );
      if (res.data?.data) {
        saveSession(res.data.data);
        return true;
      }
      return false;
    } catch {
      return false;
    } finally {
      refreshPromise = null;
    }
  })();
  return refreshPromise;
};

http.interceptors.response.use(
  (r) => r,
  async (error: AxiosError<ApiEnvelope<unknown>>) => {
    const original = error.config as (InternalAxiosRequestConfig & { _retried?: boolean }) | undefined;
    const status = error.response?.status;

    if (status === 401 && original && !original._retried && getRefreshToken()) {
      original._retried = true;
      if (await refreshSession()) {
        original.headers?.set("Authorization", `Bearer ${getToken() ?? ""}`);
        return http(original);
      }
    }

    if (status === 401) {
      const onGroupLogin = window.location.pathname.startsWith("/group");
      clearSession();
      window.location.href = onGroupLogin ? "/group/login" : "/login";
      return Promise.reject(new ApiError(401, "Unauthorized"));
    }

    const body = error.response?.data;
    throw new ApiError(status ?? 0, body?.message || error.message || "Request failed", body?.error);
  },
);

// Unwrap the ApiResponse envelope: return body.data (or body if absent).
async function unwrap<T>(p: Promise<{ data: ApiEnvelope<T> }>): Promise<T> {
  const res = await p;
  const body = res.data;
  return (body?.data !== undefined ? body.data : (body as unknown)) as T;
}

const get = <T>(path: string, params?: Record<string, unknown>) =>
  unwrap<T>(http.get(path, { params }));
const post = <T>(path: string, body?: unknown, config?: AxiosRequestConfig) =>
  unwrap<T>(http.post(path, body ?? {}, config));
const put = <T>(path: string, body?: unknown) => unwrap<T>(http.put(path, body ?? {}));
const patch = <T>(path: string, body?: unknown) => unwrap<T>(http.patch(path, body ?? {}));
const del = <T>(path: string) => unwrap<T>(http.delete(path));

async function getPdf(path: string): Promise<Blob> {
  const res = await http.get(path, { responseType: "blob" });
  return res.data as Blob;
}

// ── Domain-specific API surface (mirrors core.js) ──────────────────────────
export const api = {
  auth: {
    login: (username: string, password: string, hotelId: string) =>
      post<StaffLoginResponse>(`/hotels/${hotelId}/auth/login`, { username, password }),
    logout: () => post<void>("/auth/logout", { refreshToken: getRefreshToken() }),
    me: () => get<StaffLoginResponse>("/auth/me"),
  },

  groupAuth: {
    login: (email: string, password: string) =>
      post<GroupLoginResponse>("/group/auth/login", { email, password }),
    signup: (groupName: string, adminEmail: string, password: string) =>
      post<unknown>("/group/auth/signup", { groupName, adminEmail, password }),
    enterHotel: (hotelId: string) =>
      post<GroupLoginResponse>(`/group/hotels/${hotelId}/enter`, {}),
  },

  rooms: {
    list: (params?: Record<string, unknown>) => get<PageResult<Room>>("/rooms", params),
    get: (id: string) => get<Room>(`/rooms/${id}`),
    create: (data: Record<string, unknown>) => post<Room>("/rooms", data),
    update: (id: string, data: Record<string, unknown>) => put<Room>(`/rooms/${id}`, data),
    available: (checkIn: string, checkOut: string) =>
      get<Room[]>("/rooms/available", { checkIn, checkOut }),
  },

  roomTypes: {
    list: (params?: Record<string, unknown>) => get<PageResult<RoomType>>("/room-types", params),
    get: (id: string) => get<RoomType>(`/room-types/${id}`),
    create: (data: Record<string, unknown>) => post<RoomType>("/room-types", data),
    update: (id: string, data: Record<string, unknown>) => put<RoomType>(`/room-types/${id}`, data),
  },

  hotels: {
    completeSetupStep: (hotelId: string, step: SetupStep) =>
      post<unknown>(`/group/hotels/${hotelId}/setup/${step}`, {}),
  },

  guests: {
    list: (params?: Record<string, unknown>) => get<PageResult<Guest>>("/guests", params),
    get: (id: string) => get<Guest>(`/guests/${id}`),
    search: (q: string) => get<Guest[]>("/guests/search", { q }),
    register: (data: Partial<Guest>) => post<Guest>("/guests", data),
    update: (id: string, data: Partial<Guest>) => put<Guest>(`/guests/${id}`, data),
  },

  reservations: {
    list: (params?: Record<string, unknown>) =>
      get<PageResult<ReservationSummary>>("/reservations", params),
    get: (id: string) => get<ReservationSummary>(`/reservations/${id}`),
    create: (data: CreateReservationInput) => post<ReservationSummary>("/reservations", data),
    arrivals: () => get<ReservationSummary[]>("/reservations/today/arrivals"),
    departures: () => get<ReservationSummary[]>("/reservations/today/departures"),
    checkIn: (id: string) => patch<ReservationSummary>(`/reservations/${id}/checkin`),
    checkOut: (id: string) => patch<ReservationSummary>(`/reservations/${id}/checkout`),
    cancel: (id: string) => patch<ReservationSummary>(`/reservations/${id}/cancel`),
  },

  invoices: {
    list: (params?: Record<string, unknown>) => get<PageResult<Invoice>>("/invoices", params),
    get: (id: string) => get<Invoice>(`/invoices/${id}`),
    recordPayment: (id: string, data: PaymentInput) =>
      post<Invoice>(`/invoices/${id}/payments`, data),
    pdf: (id: string) => getPdf(`/invoices/${id}/pdf`),
  },

  staff: {
    list: (params?: Record<string, unknown>) => get<PageResult<Staff>>("/staff", params),
    get: (id: string) => get<Staff>(`/staff/${id}`),
    create: (data: Record<string, unknown>) => post<Staff>("/staff", data),
    update: (id: string, data: Record<string, unknown>) => put<Staff>(`/staff/${id}`, data),
    changePassword: (id: string, data: { currentPassword: string; newPassword: string }) =>
      patch<unknown>(`/staff/${id}/password`, data),
    toggle: (id: string) => patch<Staff>(`/staff/${id}/toggle`),
  },

  housekeeping: {
    board: () => get<HousekeepingFloor[]>("/housekeeping/board"),
    updateRoomStatus: (id: string, data: { status: RoomStatus; notes?: string | null }) =>
      patch<unknown>(`/housekeeping/rooms/${id}/status`, data),
  },

  groupDashboard: {
    get: () => get<HotelSummary[]>("/group/dashboard"),
  },

  // Raw verbs for screens not yet ported.
  raw: { get, post, put, patch, del, getPdf },
};
