import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api, ApiError } from "../lib/api";
import { useToast } from "../lib/toast";
import { formatNights, statusColor, statusLabel } from "../lib/utils";
import type { ReservationSummary } from "../lib/types";

function StatCard({ label, value, hint }: { label: string; value: number | string; hint: string }) {
  return (
    <div className="col-6 col-md-3">
      <div className="card p-3 h-100">
        <div className="text-muted small text-uppercase">{label}</div>
        <div className="fs-3 fw-bold">{value}</div>
        <small className="text-muted">{hint}</small>
      </div>
    </div>
  );
}

function StatusBadge({ status }: { status: string }) {
  return <span className={`badge bg-${statusColor(status)}`}>{statusLabel(status)}</span>;
}

export default function Dashboard() {
  const toast = useToast();
  const qc = useQueryClient();

  const rooms = useQuery({ queryKey: ["rooms", { size: 200 }], queryFn: () => api.rooms.list({ size: 200 }) });
  const arrivals = useQuery({ queryKey: ["arrivals"], queryFn: () => api.reservations.arrivals() });
  const departures = useQuery({ queryKey: ["departures"], queryFn: () => api.reservations.departures() });

  const refresh = () => {
    void qc.invalidateQueries({ queryKey: ["rooms", { size: 200 }] });
    void qc.invalidateQueries({ queryKey: ["arrivals"] });
    void qc.invalidateQueries({ queryKey: ["departures"] });
  };

  const checkIn = useMutation({
    mutationFn: (id: string) => api.reservations.checkIn(id),
    onSuccess: () => {
      toast("Guest checked in successfully");
      refresh();
    },
    onError: (e) => toast(e instanceof ApiError ? e.message : "Check-in failed", "danger"),
  });

  const checkOut = useMutation({
    mutationFn: (id: string) => api.reservations.checkOut(id),
    onSuccess: () => {
      toast("Guest checked out — invoice generated");
      refresh();
    },
    onError: (e) => toast(e instanceof ApiError ? e.message : "Check-out failed", "danger"),
  });

  const roomList = rooms.data?.content ?? [];
  const occupied = roomList.filter((r) => r.status === "OCCUPIED").length;
  const available = roomList.filter((r) => r.status === "AVAILABLE").length;
  const today = new Date().toLocaleDateString("en-GB", {
    weekday: "long", day: "2-digit", month: "long", year: "numeric",
  });

  const guestName = (r: ReservationSummary) => r.guestFullName || r.guestId;
  const roomName = (r: ReservationSummary) => r.roomNumber || r.roomId;

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center px-4 py-3 bg-white border-bottom">
        <h2 className="h4 mb-0">Dashboard</h2>
        <span className="text-muted small">{today}</span>
      </div>

      <div className="p-4">
        <div className="row g-3 mb-4">
          <StatCard label="Occupied" value={rooms.isLoading ? "—" : occupied} hint={`${roomList.length} total rooms`} />
          <StatCard label="Available" value={rooms.isLoading ? "—" : available} hint="Ready to book" />
          <StatCard label="Arrivals Today" value={arrivals.isLoading ? "—" : arrivals.data?.length ?? 0} hint="Expected check-ins" />
          <StatCard label="Departures Today" value={departures.isLoading ? "—" : departures.data?.length ?? 0} hint="Expected check-outs" />
        </div>

        <div className="row g-4">
          <div className="col-12 col-lg-6">
            <div className="card">
              <div className="card-header">Today's Arrivals</div>
              <div className="table-responsive">
                <table className="table table-hover mb-0">
                  <thead>
                    <tr><th>Guest</th><th>Room</th><th>Nights</th><th>Status</th><th></th></tr>
                  </thead>
                  <tbody>
                    {(arrivals.data ?? []).map((r) => (
                      <tr key={r.id}>
                        <td><strong>{guestName(r)}</strong></td>
                        <td>{roomName(r)}</td>
                        <td>{formatNights(r.nights)}</td>
                        <td><StatusBadge status={r.status} /></td>
                        <td>
                          {r.status === "CONFIRMED" && (
                            <button
                              className="btn btn-sm btn-primary"
                              disabled={checkIn.isPending}
                              onClick={() => checkIn.mutate(r.id)}
                            >
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
            <div className="card">
              <div className="card-header">Today's Departures</div>
              <div className="table-responsive">
                <table className="table table-hover mb-0">
                  <thead>
                    <tr><th>Guest</th><th>Room</th><th>Balance</th><th>Status</th><th></th></tr>
                  </thead>
                  <tbody>
                    {(departures.data ?? []).map((r) => (
                      <tr key={r.id}>
                        <td><strong>{guestName(r)}</strong></td>
                        <td>{roomName(r)}</td>
                        <td><span className="text-warning small">In-house</span></td>
                        <td><StatusBadge status={r.status} /></td>
                        <td>
                          <button
                            className="btn btn-sm btn-outline-secondary"
                            disabled={checkOut.isPending}
                            onClick={() => checkOut.mutate(r.id)}
                          >
                            Check Out
                          </button>
                        </td>
                      </tr>
                    ))}
                    {!departures.isLoading && (departures.data?.length ?? 0) === 0 && (
                      <tr><td colSpan={5} className="text-center text-muted py-3">No departures today</td></tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
