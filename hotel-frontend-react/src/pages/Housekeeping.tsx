import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api, ApiError } from "../lib/api";
import { useToast } from "../lib/toast";
import { statusColor, statusLabel } from "../lib/utils";
import PageHeader from "../components/PageHeader";
import type { HkRoom, RoomStatus } from "../lib/types";

// Valid target statuses per source state — matches backend transition rules.
const TRANSITIONS: Record<RoomStatus, { value: RoomStatus; label: string }[]> = {
  AVAILABLE: [{ value: "MAINTENANCE", label: "Maintenance" }],
  OCCUPIED: [
    { value: "SERVICE_REQUESTED", label: "Service Requested (Guest Cleaning)" },
    { value: "MAINTENANCE", label: "Maintenance" },
  ],
  DIRTY: [
    { value: "AVAILABLE", label: "Available (Clean)" },
    { value: "MAINTENANCE", label: "Maintenance" },
  ],
  MAINTENANCE: [{ value: "AVAILABLE", label: "Available (Clean)" }],
  SERVICE_REQUESTED: [{ value: "OCCUPIED", label: "Service Complete (Back to Occupied)" }],
};

const LEGEND: { status: RoomStatus; label: string }[] = [
  { status: "AVAILABLE", label: "Available" },
  { status: "OCCUPIED", label: "Occupied" },
  { status: "DIRTY", label: "Dirty" },
  { status: "MAINTENANCE", label: "Maintenance" },
  { status: "SERVICE_REQUESTED", label: "Service Req." },
];

export default function Housekeeping() {
  const toast = useToast();
  const qc = useQueryClient();
  const [selected, setSelected] = useState<HkRoom | null>(null);
  const [target, setTarget] = useState<RoomStatus | "">("");
  const [notes, setNotes] = useState("");

  const { data, isLoading } = useQuery({
    queryKey: ["hk-board"],
    queryFn: () => api.housekeeping.board(),
  });

  const update = useMutation({
    mutationFn: (room: HkRoom) =>
      api.housekeeping.updateRoomStatus(room.roomId, { status: target as RoomStatus, notes: notes || null }),
    onSuccess: () => {
      toast("Room status updated");
      setSelected(null);
      void qc.invalidateQueries({ queryKey: ["hk-board"] });
    },
    onError: (e) => toast(e instanceof ApiError ? e.message : "Update failed", "danger"),
  });

  const openRoom = (room: HkRoom) => {
    const options = TRANSITIONS[room.status] ?? [];
    setSelected(room);
    setTarget(options[0]?.value ?? "");
    setNotes("");
  };

  const options = selected ? TRANSITIONS[selected.status] ?? [] : [];

  return (
    <div>
      <PageHeader
        title="Housekeeping Board"
        action={
          <div className="d-none d-md-flex gap-1">
            {LEGEND.map((l) => (
              <span key={l.status} className={`badge bg-${statusColor(l.status)}`}>● {l.label}</span>
            ))}
          </div>
        }
      />
      <div className="p-4">
        {isLoading && <div className="text-muted">Loading board…</div>}
        {(data ?? []).map((floor) => (
          <div key={floor.floor} className="mb-4">
            <div className="text-muted text-uppercase mb-2" style={{ fontSize: 10, letterSpacing: 2 }}>
              Floor {floor.floor}
            </div>
            <div className="d-flex flex-wrap gap-2">
              {floor.rooms.map((r) => (
                <div
                  key={r.roomId}
                  role="button"
                  onClick={() => openRoom(r)}
                  className="card p-2 text-center"
                  style={{ width: 120, borderTop: `3px solid var(--bs-${statusColor(r.status)})`, cursor: "pointer" }}
                >
                  <div className="fw-bold">{r.roomNumber}</div>
                  <div className="text-muted" style={{ fontSize: 11 }}>{r.roomTypeName}</div>
                  <div className="mt-1">
                    <span className={`badge bg-${statusColor(r.status)}`}>{statusLabel(r.status)}</span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        ))}
        {!isLoading && (data?.length ?? 0) === 0 && (
          <div className="text-muted text-center py-5">No rooms to display</div>
        )}
      </div>

      {selected && (
        <>
          <div className="modal d-block" tabIndex={-1}>
            <div className="modal-dialog">
              <div className="modal-content">
                <div className="modal-header">
                  <h5 className="modal-title">Room {selected.roomNumber}</h5>
                  <button type="button" className="btn-close" onClick={() => setSelected(null)} />
                </div>
                <div className="modal-body">
                  <div className="mb-3">
                    <label className="form-label">New Status</label>
                    <select
                      className="form-select"
                      value={target}
                      onChange={(e) => setTarget(e.target.value as RoomStatus)}
                      disabled={options.length === 0}
                    >
                      {options.map((o) => (
                        <option key={o.value} value={o.value}>{o.label}</option>
                      ))}
                    </select>
                    {options.length === 0 && (
                      <div className="form-text">No status changes available from {statusLabel(selected.status)}.</div>
                    )}
                  </div>
                  <div className="mb-3">
                    <label className="form-label">Notes (optional)</label>
                    <textarea
                      className="form-control"
                      rows={2}
                      placeholder="e.g. broken AC, needs towels…"
                      value={notes}
                      onChange={(e) => setNotes(e.target.value)}
                    />
                  </div>
                </div>
                <div className="modal-footer">
                  <button className="btn btn-outline-secondary" onClick={() => setSelected(null)}>Cancel</button>
                  <button
                    className="btn btn-primary"
                    disabled={options.length === 0 || update.isPending}
                    onClick={() => update.mutate(selected)}
                  >
                    Update
                  </button>
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
