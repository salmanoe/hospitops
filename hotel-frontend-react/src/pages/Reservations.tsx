import { useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api, ApiError } from "../lib/api";
import { useToast } from "../lib/toast";
import { formatDate, formatNights, statusColor } from "../lib/utils";
import PageHeader from "../components/PageHeader";
import Pagination from "../components/Pagination";
import StatusBadge from "../components/StatusBadge";
import type { ReservationStatus, ReservationSummary, Room } from "../lib/types";

const FILTERS: { label: string; status: ReservationStatus | "" }[] = [
  { label: "All", status: "" },
  { label: "Confirmed", status: "CONFIRMED" },
  { label: "In-House", status: "CHECKED_IN" },
  { label: "Checked Out", status: "CHECKED_OUT" },
  { label: "Cancelled", status: "CANCELLED" },
];

// ── Calendar helpers ────────────────────────────────────────────────────────
const DAYS = 14;
const DOW = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
const ACTIVE_CAL: ReservationSummary["status"][] = ["PENDING", "CONFIRMED", "CHECKED_IN"];

const addDays = (d: Date, n: number) => {
  const x = new Date(d);
  x.setDate(x.getDate() + n);
  return x;
};
const iso = (d: Date) => d.toISOString().slice(0, 10);
const isWeekend = (d: Date) => d.getDay() === 0 || d.getDay() === 6;

const guestName = (r: ReservationSummary) => r.guestFullName || r.guestId;
const roomLabel = (r: ReservationSummary) => r.roomNumber || r.roomId;

type View = "calendar" | "list";

export default function Reservations() {
  const toast = useToast();
  const qc = useQueryClient();
  const navigate = useNavigate();
  const [view, setView] = useState<View>("calendar");

  // ── Guide header data (operational summary + today's movements) ──────────
  const rooms = useQuery({ queryKey: ["rooms", { size: 200 }], queryFn: () => api.rooms.list({ size: 200 }) });
  const arrivals = useQuery({ queryKey: ["arrivals"], queryFn: () => api.reservations.arrivals() });
  const departures = useQuery({ queryKey: ["departures"], queryFn: () => api.reservations.departures() });

  const roomList = rooms.data?.content ?? [];
  const occupied = roomList.filter((r) => r.status === "OCCUPIED").length;
  const available = roomList.filter((r) => r.status === "AVAILABLE").length;
  const today = new Date().toLocaleDateString("en-GB", {
    weekday: "long", day: "2-digit", month: "long", year: "numeric",
  });

  const refreshOps = () => {
    void qc.invalidateQueries({ queryKey: ["rooms", { size: 200 }] });
    void qc.invalidateQueries({ queryKey: ["arrivals"] });
    void qc.invalidateQueries({ queryKey: ["departures"] });
  };

  const checkIn = useMutation({
    mutationFn: (id: string) => api.reservations.checkIn(id),
    onSuccess: () => { toast("Guest checked in"); refreshOps(); void invalidateList(); },
    onError: (e) => toast(e instanceof ApiError ? e.message : "Check-in failed", "danger"),
  });
  const checkOut = useMutation({
    mutationFn: (id: string) => api.reservations.checkOut(id),
    onSuccess: () => { toast("Guest checked out — invoice generated"); refreshOps(); void invalidateList(); },
    onError: (e) => toast(e instanceof ApiError ? e.message : "Check-out failed", "danger"),
  });

  // ── List view data ───────────────────────────────────────────────────────
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState<ReservationStatus | "">("");
  const invalidateList = () => qc.invalidateQueries({ queryKey: ["reservations"] });

  const list = useQuery({
    queryKey: ["reservations", "list", page, status],
    queryFn: () => api.reservations.list({ page, size: 20, ...(status ? { status } : {}) }),
    placeholderData: keepPreviousData,
    enabled: view === "list",
  });
  const rows = list.data?.content ?? [];

  // ── Calendar view data ───────────────────────────────────────────────────
  const [start, setStart] = useState(() => {
    const d = new Date();
    d.setHours(0, 0, 0, 0);
    return d;
  });
  const calDays = useMemo(() => Array.from({ length: DAYS }, (_, i) => addDays(start, i)), [start]);
  const from = iso(calDays[0]);
  const to = iso(calDays[DAYS - 1]);

  // Server-side date-range fetch — only the reservations overlapping the
  // visible window come back, not the whole table.
  const calRes = useQuery({
    queryKey: ["reservations", "calendar", from, to],
    queryFn: () => api.reservations.range(from, to),
    enabled: view === "calendar",
  });

  const sortedRooms: Room[] = useMemo(
    () => [...roomList].sort((a, b) => a.roomNumber.localeCompare(b.roomNumber, undefined, { numeric: true })),
    [roomList],
  );

  const byRoom = useMemo(() => {
    const map = new Map<string, ReservationSummary[]>();
    for (const r of calRes.data ?? []) {
      if (!r.checkInDate || !r.checkOutDate) continue;
      if (!ACTIVE_CAL.includes(r.status)) continue;
      if (r.checkOutDate <= from || r.checkInDate > to) continue; // no overlap
      const arr = map.get(r.roomId) ?? [];
      arr.push(r);
      map.set(r.roomId, arr);
    }
    for (const arr of map.values()) arr.sort((a, b) => (a.checkInDate! < b.checkInDate! ? -1 : 1));
    return map;
  }, [calRes.data, from, to]);

  const stickyCol: React.CSSProperties = { position: "sticky", left: 0, zIndex: 1, background: "#fff", minWidth: 120 };
  const cellStyle: React.CSSProperties = { minWidth: 56, textAlign: "center", padding: "2px 4px" };

  const renderRoomRow = (room: Room) => {
    const bookings = byRoom.get(room.id) ?? [];
    const cells = [];
    let i = 0;
    while (i < DAYS) {
      const dISO = iso(calDays[i]);
      const res = bookings.find((r) => r.checkInDate! <= dISO && dISO < r.checkOutDate!);
      if (res) {
        let span = 0;
        while (i + span < DAYS && iso(calDays[i + span]) < res.checkOutDate!) span++;
        if (span < 1) span = 1;
        cells.push(
          <td key={dISO} colSpan={span} style={{ ...cellStyle, padding: 2 }}>
            <button
              className={`btn btn-sm w-100 text-truncate text-white bg-${statusColor(res.status)}`}
              style={{ border: 0 }}
              title={`${res.guestFullName ?? "Guest"} · ${res.reservationNumber ?? ""} · ${res.status}`}
              onClick={() => navigate(`/reservations/${res.id}`)}
            >
              {res.guestFullName ?? res.reservationNumber ?? "Booking"}
            </button>
          </td>,
        );
        i += span;
      } else {
        cells.push(
          <td
            key={dISO}
            style={{ ...cellStyle, cursor: "pointer", background: isWeekend(calDays[i]) ? "#faf8f4" : undefined }}
            title="New reservation"
            onClick={() => navigate("/reservations/new")}
          />,
        );
        i++;
      }
    }
    return (
      <tr key={room.id}>
        <td style={stickyCol}>
          <strong>{room.roomNumber}</strong>
          {room.roomTypeName ? <div className="text-muted small">{room.roomTypeName}</div> : null}
        </td>
        {cells}
      </tr>
    );
  };

  return (
    <div>
      <PageHeader
        title="Reservations"
        action={<Link to="/reservations/new" className="btn btn-primary btn-sm">＋ New Booking</Link>}
      />

      <div className="p-4">
        {/* ── Guide header: at-a-glance + today's movements ─────────────── */}
        <div className="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2">
          <span className="text-muted small">{today}</span>
          <div className="d-flex gap-3 flex-wrap small">
            <span><strong>{rooms.isLoading ? "—" : occupied}</strong> occupied</span>
            <span><strong>{rooms.isLoading ? "—" : available}</strong> available</span>
            <span><strong>{arrivals.isLoading ? "—" : arrivals.data?.length ?? 0}</strong> arrivals</span>
            <span><strong>{departures.isLoading ? "—" : departures.data?.length ?? 0}</strong> departures</span>
          </div>
        </div>

        <div className="row g-3 mb-4">
          <div className="col-12 col-lg-6">
            <div className="card h-100">
              <div className="card-header">Today's Arrivals</div>
              <div className="table-responsive">
                <table className="table table-hover table-sm mb-0">
                  <thead>
                    <tr><th>Guest</th><th>Room</th><th>Nights</th><th>Status</th><th></th></tr>
                  </thead>
                  <tbody>
                    {(arrivals.data ?? []).map((r) => (
                      <tr key={r.id}>
                        <td><strong>{guestName(r)}</strong></td>
                        <td>{roomLabel(r)}</td>
                        <td>{formatNights(r.nights)}</td>
                        <td><StatusBadge status={r.status} /></td>
                        <td className="text-end">
                          {r.status === "CONFIRMED" && (
                            <button className="btn btn-sm btn-primary" disabled={checkIn.isPending} onClick={() => checkIn.mutate(r.id)}>
                              Check In
                            </button>
                          )}
                        </td>
                      </tr>
                    ))}
                    {!arrivals.isLoading && (arrivals.data?.length ?? 0) === 0 && (
                      <tr><td colSpan={5} className="text-center text-muted py-3">No arrivals today</td></tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          <div className="col-12 col-lg-6">
            <div className="card h-100">
              <div className="card-header">Today's Departures</div>
              <div className="table-responsive">
                <table className="table table-hover table-sm mb-0">
                  <thead>
                    <tr><th>Guest</th><th>Room</th><th>Status</th><th></th></tr>
                  </thead>
                  <tbody>
                    {(departures.data ?? []).map((r) => (
                      <tr key={r.id}>
                        <td><strong>{guestName(r)}</strong></td>
                        <td>{roomLabel(r)}</td>
                        <td><StatusBadge status={r.status} /></td>
                        <td className="text-end">
                          <button className="btn btn-sm btn-outline-secondary" disabled={checkOut.isPending} onClick={() => checkOut.mutate(r.id)}>
                            Check Out
                          </button>
                        </td>
                      </tr>
                    ))}
                    {!departures.isLoading && (departures.data?.length ?? 0) === 0 && (
                      <tr><td colSpan={4} className="text-center text-muted py-3">No departures today</td></tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>

        {/* ── View toggle ──────────────────────────────────────────────── */}
        <div className="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2">
          <div className="btn-group btn-group-sm" role="group" aria-label="Reservations view">
            <button className={"btn " + (view === "calendar" ? "btn-primary" : "btn-outline-secondary")} onClick={() => setView("calendar")}>
              Calendar
            </button>
            <button className={"btn " + (view === "list" ? "btn-primary" : "btn-outline-secondary")} onClick={() => setView("list")}>
              List
            </button>
          </div>

          {view === "calendar" ? (
            <div className="d-flex gap-2 align-items-center flex-wrap">
              <button className="btn btn-outline-secondary btn-sm" onClick={() => setStart(addDays(start, -DAYS))}>← Prev</button>
              <span className="small text-muted">{from} → {to}</span>
              <button className="btn btn-outline-secondary btn-sm" onClick={() => setStart(addDays(start, DAYS))}>Next →</button>
              <button className="btn btn-outline-secondary btn-sm" onClick={() => { const d = new Date(); d.setHours(0,0,0,0); setStart(d); }}>Today</button>
            </div>
          ) : (
            <div className="d-flex gap-2 flex-wrap">
              {FILTERS.map((f) => (
                <button
                  key={f.label}
                  className={"btn btn-sm " + (status === f.status ? "btn-primary" : "btn-outline-secondary")}
                  onClick={() => { setStatus(f.status); setPage(0); }}
                >
                  {f.label}
                </button>
              ))}
            </div>
          )}
        </div>

        {/* ── Calendar view ────────────────────────────────────────────── */}
        {view === "calendar" && (
          <>
            <div className="card">
              <div className="table-responsive">
                <table className="table table-bordered mb-0" style={{ fontSize: ".85rem" }}>
                  <thead>
                    <tr>
                      <th style={stickyCol}>Room</th>
                      {calDays.map((d) => (
                        <th key={iso(d)} style={{ ...cellStyle, background: isWeekend(d) ? "#f6f3ee" : undefined }}>
                          <div className="text-muted">{DOW[d.getDay()]}</div>
                          <div>{d.getDate()}/{d.getMonth() + 1}</div>
                        </th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {sortedRooms.map(renderRoomRow)}
                    {sortedRooms.length === 0 && (
                      <tr><td colSpan={DAYS + 1} className="text-center text-muted py-4">No rooms yet</td></tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>
            <p className="text-muted small mt-2">Click a booking to open it; click an empty night to create a reservation.</p>
          </>
        )}

        {/* ── List view ────────────────────────────────────────────────── */}
        {view === "list" && (
          <div className="card">
            <div className="table-responsive">
              <table className="table table-hover mb-0">
                <thead>
                  <tr>
                    <th>No.</th><th>Guest</th><th>Room</th>
                    <th>Check-In</th><th>Check-Out</th>
                    <th>Nights</th><th>Status</th><th></th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((r) => (
                    <tr key={r.id}>
                      <td><small className="text-muted">{r.reservationNumber}</small></td>
                      <td>{r.guestFullName || r.guestId}</td>
                      <td>{r.roomNumber || r.roomId}</td>
                      <td>{formatDate(r.checkInDate)}</td>
                      <td>{formatDate(r.checkOutDate)}</td>
                      <td>{formatNights(r.nights)}</td>
                      <td><StatusBadge status={r.status} /></td>
                      <td>
                        <div className="d-flex gap-1">
                          <Link to={`/reservations/${r.id}`} className="btn btn-outline-secondary btn-sm">View</Link>
                          {r.status === "CONFIRMED" && (
                            <button className="btn btn-primary btn-sm" disabled={checkIn.isPending} onClick={() => checkIn.mutate(r.id)}>
                              Check In
                            </button>
                          )}
                          {r.status === "CHECKED_IN" && (
                            <button className="btn btn-success btn-sm" disabled={checkOut.isPending} onClick={() => checkOut.mutate(r.id)}>
                              Check Out
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                  {!list.isLoading && rows.length === 0 && (
                    <tr><td colSpan={8} className="text-center text-muted py-4">No reservations found</td></tr>
                  )}
                </tbody>
              </table>
            </div>
            {list.data && (
              <div className="card-body pt-2">
                <Pagination page={list.data.page} totalPages={list.data.totalPages} onChange={setPage} />
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
