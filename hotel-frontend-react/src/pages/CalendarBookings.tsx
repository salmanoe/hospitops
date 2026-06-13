import { useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { api } from "../lib/api";
import { statusColor } from "../lib/utils";
import PageHeader from "../components/PageHeader";
import type { ReservationSummary, Room } from "../lib/types";

const DAYS = 14;
const DOW = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];

const addDays = (d: Date, n: number) => {
  const x = new Date(d);
  x.setDate(x.getDate() + n);
  return x;
};
const iso = (d: Date) => d.toISOString().slice(0, 10);
const isWeekend = (d: Date) => d.getDay() === 0 || d.getDay() === 6;

const ACTIVE: ReservationSummary["status"][] = ["PENDING", "CONFIRMED", "CHECKED_IN"];

export default function CalendarBookings() {
  const navigate = useNavigate();
  const [start, setStart] = useState(() => {
    const d = new Date();
    d.setHours(0, 0, 0, 0);
    return d;
  });

  const days = useMemo(() => Array.from({ length: DAYS }, (_, i) => addDays(start, i)), [start]);
  const from = iso(days[0]);
  const to = iso(days[DAYS - 1]);

  const roomsQ = useQuery({ queryKey: ["rooms", { size: 200 }], queryFn: () => api.rooms.list({ size: 200 }) });
  const resQ = useQuery({ queryKey: ["reservations", { size: 500 }], queryFn: () => api.reservations.list({ size: 500 }) });

  const rooms: Room[] = useMemo(
    () => [...(roomsQ.data?.content ?? [])].sort((a, b) => a.roomNumber.localeCompare(b.roomNumber, undefined, { numeric: true })),
    [roomsQ.data],
  );

  // Active reservations overlapping the window, grouped by room.
  const byRoom = useMemo(() => {
    const map = new Map<string, ReservationSummary[]>();
    for (const r of resQ.data?.content ?? []) {
      if (!r.checkInDate || !r.checkOutDate) continue;
      if (!ACTIVE.includes(r.status)) continue;
      if (r.checkOutDate <= from || r.checkInDate > to) continue; // no overlap
      const list = map.get(r.roomId) ?? [];
      list.push(r);
      map.set(r.roomId, list);
    }
    for (const list of map.values()) list.sort((a, b) => (a.checkInDate! < b.checkInDate! ? -1 : 1));
    return map;
  }, [resQ.data, from, to]);

  const stickyCol: React.CSSProperties = { position: "sticky", left: 0, zIndex: 1, background: "#fff", minWidth: 120 };
  const cellStyle: React.CSSProperties = { minWidth: 56, textAlign: "center", padding: "2px 4px" };

  const renderRoomRow = (room: Room) => {
    const list = byRoom.get(room.id) ?? [];
    const cells = [];
    let i = 0;
    while (i < DAYS) {
      const dISO = iso(days[i]);
      const res = list.find((r) => r.checkInDate! <= dISO && dISO < r.checkOutDate!);
      if (res) {
        let span = 0;
        while (i + span < DAYS && iso(days[i + span]) < res.checkOutDate!) span++;
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
            style={{ ...cellStyle, cursor: "pointer", background: isWeekend(days[i]) ? "#faf8f4" : undefined }}
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
        title="Booking Calendar"
        action={
          <div className="d-flex gap-2 align-items-center">
            <button className="btn btn-outline-secondary btn-sm" onClick={() => setStart(addDays(start, -DAYS))}>← Prev</button>
            <span className="small text-muted">{from} → {to}</span>
            <button className="btn btn-outline-secondary btn-sm" onClick={() => setStart(addDays(start, DAYS))}>Next →</button>
            <button className="btn btn-outline-secondary btn-sm" onClick={() => { const d = new Date(); d.setHours(0,0,0,0); setStart(d); }}>Today</button>
            <button className="btn btn-primary btn-sm" onClick={() => navigate("/reservations/new")}>＋ New</button>
          </div>
        }
      />

      <div className="p-4">
        <div className="card">
          <div className="table-responsive">
            <table className="table table-bordered mb-0" style={{ fontSize: ".85rem" }}>
              <thead>
                <tr>
                  <th style={stickyCol}>Room</th>
                  {days.map((d) => (
                    <th key={iso(d)} style={{ ...cellStyle, background: isWeekend(d) ? "#f6f3ee" : undefined }}>
                      <div className="text-muted">{DOW[d.getDay()]}</div>
                      <div>{d.getDate()}/{d.getMonth() + 1}</div>
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {rooms.map(renderRoomRow)}
                {rooms.length === 0 && (
                  <tr><td colSpan={DAYS + 1} className="text-center text-muted py-4">No rooms yet</td></tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
        <p className="text-muted small mt-2">Click a booking to open it; click an empty night to create a reservation.</p>
      </div>
    </div>
  );
}
