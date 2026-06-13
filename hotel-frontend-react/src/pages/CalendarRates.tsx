import { Fragment, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api, ApiError } from "../lib/api";
import { useToast } from "../lib/toast";
import { formatRp } from "../lib/utils";
import PageHeader from "../components/PageHeader";
import type { RoomCalendarRow } from "../lib/types";

const DAYS = 14;
const DOW = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];

const addDays = (d: Date, n: number) => {
  const x = new Date(d);
  x.setDate(x.getDate() + n);
  return x;
};
const iso = (d: Date) => d.toISOString().slice(0, 10);
const isWeekend = (d: Date) => d.getDay() === 0 || d.getDay() === 6;

interface RateEdit {
  roomTypeId: string;
  name: string;
  date: string;
  rate: number;
  through: string;
}

export default function CalendarRates() {
  const toast = useToast();
  const qc = useQueryClient();
  const [start, setStart] = useState(() => {
    const d = new Date();
    d.setHours(0, 0, 0, 0);
    return d;
  });
  const [edit, setEdit] = useState<RateEdit | null>(null);

  const days = useMemo(() => Array.from({ length: DAYS }, (_, i) => addDays(start, i)), [start]);
  const from = iso(days[0]);
  const to = iso(days[DAYS - 1]);

  const { data, isLoading } = useQuery({
    queryKey: ["rooms", "calendar", from, to],
    queryFn: () => api.rooms.calendar(from, to),
  });

  const rows: RoomCalendarRow[] = data ?? [];
  // Index each room type's days by date for O(1) cell lookup.
  const byDate = useMemo(() => {
    const map = new Map<string, Map<string, { available: number; rate: number }>>();
    for (const r of rows) {
      const inner = new Map<string, { available: number; rate: number }>();
      for (const d of r.days) inner.set(d.date, { available: d.available, rate: d.rate });
      map.set(r.roomTypeId, inner);
    }
    return map;
  }, [rows]);

  const saveRate = useMutation({
    mutationFn: (e: RateEdit) =>
      api.roomTypes.addRate(e.roomTypeId, {
        name: `Calendar ${e.date}`,
        priceOverride: e.rate,
        validFrom: e.date,
        validUntil: e.through < e.date ? e.date : e.through,
      }),
    onSuccess: () => {
      toast("Rate updated");
      setEdit(null);
      void qc.invalidateQueries({ queryKey: ["rooms", "calendar"] });
    },
    onError: (err) => toast(err instanceof ApiError ? err.message : "Save failed", "danger"),
  });

  const cellStyle: React.CSSProperties = { minWidth: 64, textAlign: "center" };
  const stickyCol: React.CSSProperties = {
    position: "sticky", left: 0, zIndex: 1, background: "#fff", minWidth: 150,
  };

  return (
    <div>
      <PageHeader
        title="Rates & Availability"
        action={
          <div className="d-flex gap-2 align-items-center">
            <button className="btn btn-outline-secondary btn-sm" onClick={() => setStart(addDays(start, -DAYS))}>← Prev</button>
            <span className="small text-muted">{from} → {to}</span>
            <button className="btn btn-outline-secondary btn-sm" onClick={() => setStart(addDays(start, DAYS))}>Next →</button>
            <button className="btn btn-outline-secondary btn-sm" onClick={() => { const d = new Date(); d.setHours(0,0,0,0); setStart(d); }}>Today</button>
          </div>
        }
      />

      <div className="p-4">
        <div className="card">
          <div className="table-responsive">
            <table className="table table-bordered mb-0" style={{ fontSize: ".85rem" }}>
              <thead>
                <tr>
                  <th style={stickyCol}>Room type</th>
                  {days.map((d) => (
                    <th key={iso(d)} style={{ ...cellStyle, background: isWeekend(d) ? "#f6f3ee" : undefined }}>
                      <div className="text-muted">{DOW[d.getDay()]}</div>
                      <div>{d.getDate()}/{d.getMonth() + 1}</div>
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {rows.map((r) => {
                  const cells = byDate.get(r.roomTypeId);
                  return (
                    <Fragment key={r.roomTypeId}>
                      <tr>
                        <td style={{ ...stickyCol, verticalAlign: "middle" }} rowSpan={2}>
                          <strong>{r.name}</strong>
                          <div className="text-muted small">{r.capacity} pax</div>
                        </td>
                        {days.map((d) => {
                          const c = cells?.get(iso(d));
                          const avail = c?.available ?? 0;
                          return (
                            <td key={iso(d)} style={{ ...cellStyle, background: isWeekend(d) ? "#faf8f4" : undefined }}>
                              <span className={`badge bg-${avail === 0 ? "danger" : avail <= 2 ? "warning" : "success"}`}>
                                {avail}
                              </span>
                            </td>
                          );
                        })}
                      </tr>
                      <tr>
                        {days.map((d) => {
                          const c = cells?.get(iso(d));
                          const rate = c?.rate ?? 0;
                          return (
                            <td
                              key={iso(d)}
                              style={{ ...cellStyle, cursor: "pointer", background: isWeekend(d) ? "#faf8f4" : undefined }}
                              className="text-nowrap"
                              title="Click to set rate"
                              onClick={() => setEdit({ roomTypeId: r.roomTypeId, name: r.name, date: iso(d), rate, through: iso(d) })}
                            >
                              {formatRp(rate)}
                            </td>
                          );
                        })}
                      </tr>
                    </Fragment>
                  );
                })}
                {!isLoading && rows.length === 0 && (
                  <tr><td colSpan={DAYS + 1} className="text-center text-muted py-4">No room types yet</td></tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
        <p className="text-muted small mt-2">
          Availability is derived from inventory and bookings. Click a rate cell to set a price; rate changes sync to connected channels.
        </p>
      </div>

      {edit && (
        <>
          <div className="modal d-block" tabIndex={-1}>
            <div className="modal-dialog">
              <div className="modal-content">
                <div className="modal-header">
                  <h5 className="modal-title">Set rate — {edit.name}</h5>
                  <button type="button" className="btn-close" onClick={() => setEdit(null)} />
                </div>
                <div className="modal-body">
                  <div className="mb-3">
                    <label className="form-label">Rate / night (IDR) *</label>
                    <input
                      type="number" className="form-control" min={0} step={10000}
                      value={edit.rate}
                      onChange={(e) => setEdit({ ...edit, rate: Number(e.target.value) })}
                    />
                  </div>
                  <div className="row g-2">
                    <div className="col">
                      <label className="form-label small">From</label>
                      <input type="date" className="form-control" value={edit.date}
                        onChange={(e) => setEdit({ ...edit, date: e.target.value })} />
                    </div>
                    <div className="col">
                      <label className="form-label small">Through</label>
                      <input type="date" className="form-control" value={edit.through} min={edit.date}
                        onChange={(e) => setEdit({ ...edit, through: e.target.value })} />
                    </div>
                  </div>
                  <div className="form-text">Applies the rate to every night in the range.</div>
                </div>
                <div className="modal-footer">
                  <button className="btn btn-outline-secondary" onClick={() => setEdit(null)}>Cancel</button>
                  <button className="btn btn-primary" disabled={saveRate.isPending} onClick={() => saveRate.mutate(edit)}>Save</button>
                </div>
              </div>
            </div>
          </div>
          <div className="modal-backdrop show" />
        </>
      )}
    </div>
  );
}
