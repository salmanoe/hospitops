import { useState } from "react";
import { Link } from "react-router-dom";
import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api, ApiError } from "../lib/api";
import { useToast } from "../lib/toast";
import { formatDate, formatNights } from "../lib/utils";
import PageHeader from "../components/PageHeader";
import Pagination from "../components/Pagination";
import StatusBadge from "../components/StatusBadge";
import type { ReservationStatus } from "../lib/types";

const FILTERS: { label: string; status: ReservationStatus | "" }[] = [
  { label: "All", status: "" },
  { label: "Confirmed", status: "CONFIRMED" },
  { label: "In-House", status: "CHECKED_IN" },
  { label: "Checked Out", status: "CHECKED_OUT" },
  { label: "Cancelled", status: "CANCELLED" },
];

export default function Reservations() {
  const toast = useToast();
  const qc = useQueryClient();
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState<ReservationStatus | "">("");

  const { data, isLoading } = useQuery({
    queryKey: ["reservations", page, status],
    queryFn: () => api.reservations.list({ page, size: 20, ...(status ? { status } : {}) }),
    placeholderData: keepPreviousData,
  });

  const invalidate = () => qc.invalidateQueries({ queryKey: ["reservations"] });

  const checkIn = useMutation({
    mutationFn: (id: string) => api.reservations.checkIn(id),
    onSuccess: () => { toast("Guest checked in"); void invalidate(); },
    onError: (e) => toast(e instanceof ApiError ? e.message : "Check-in failed", "danger"),
  });
  const checkOut = useMutation({
    mutationFn: (id: string) => api.reservations.checkOut(id),
    onSuccess: () => { toast("Guest checked out — invoice generated"); void invalidate(); },
    onError: (e) => toast(e instanceof ApiError ? e.message : "Check-out failed", "danger"),
  });

  const rows = data?.content ?? [];

  return (
    <div>
      <PageHeader
        title="Reservations"
        action={<Link to="/reservations/new" className="btn btn-primary btn-sm">＋ New Booking</Link>}
      />
      <div className="p-4">
        <div className="d-flex gap-2 flex-wrap mb-3">
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
                {!isLoading && rows.length === 0 && (
                  <tr><td colSpan={8} className="text-center text-muted py-4">No reservations found</td></tr>
                )}
              </tbody>
            </table>
          </div>
          {data && (
            <div className="card-body pt-2">
              <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
