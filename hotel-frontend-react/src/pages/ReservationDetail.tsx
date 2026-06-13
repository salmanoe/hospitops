import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api, ApiError } from "../lib/api";
import { useToast } from "../lib/toast";
import { formatDate, formatDateTime, formatNights, formatRp } from "../lib/utils";
import PageHeader from "../components/PageHeader";
import StatusBadge from "../components/StatusBadge";

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="col-sm-6">
      <div className="text-muted small">{label}</div>
      <div>{children}</div>
    </div>
  );
}

export default function ReservationDetail() {
  const { id = "" } = useParams();
  const toast = useToast();
  const qc = useQueryClient();
  const [confirmCancel, setConfirmCancel] = useState(false);

  const { data: r, isLoading } = useQuery({
    queryKey: ["reservation", id],
    queryFn: () => api.reservations.get(id),
  });

  const invalidate = () => {
    void qc.invalidateQueries({ queryKey: ["reservation", id] });
    void qc.invalidateQueries({ queryKey: ["reservations"] });
  };

  const checkIn = useMutation({
    mutationFn: () => api.reservations.checkIn(id),
    onSuccess: () => { toast("Guest checked in successfully"); invalidate(); },
    onError: (e) => toast(e instanceof ApiError ? e.message : "Check-in failed", "danger"),
  });
  const checkOut = useMutation({
    mutationFn: () => api.reservations.checkOut(id),
    onSuccess: () => { toast("Guest checked out — invoice generated"); invalidate(); },
    onError: (e) => toast(e instanceof ApiError ? e.message : "Check-out failed", "danger"),
  });
  const cancel = useMutation({
    mutationFn: () => api.reservations.cancel(id),
    onSuccess: () => { toast("Reservation cancelled"); setConfirmCancel(false); invalidate(); },
    onError: (e) => toast(e instanceof ApiError ? e.message : "Cancel failed", "danger"),
  });

  const adults = r?.adults ?? 0;
  const children = r?.children ?? 0;
  const guests = [`${adults} adult${adults !== 1 ? "s" : ""}`];
  if (children > 0) guests.push(`${children} child${children !== 1 ? "ren" : ""}`);

  return (
    <div>
      <PageHeader
        title={r?.reservationNumber || "Reservation"}
        action={
          <div className="d-flex gap-2">
            <Link to="/reservations" className="btn btn-outline-secondary btn-sm">← Back</Link>
            {r?.status === "CONFIRMED" && (
              <>
                <button className="btn btn-primary btn-sm" disabled={checkIn.isPending} onClick={() => checkIn.mutate()}>
                  Check In
                </button>
                <button className="btn btn-outline-danger btn-sm" onClick={() => setConfirmCancel(true)}>
                  Cancel
                </button>
              </>
            )}
            {r?.status === "CHECKED_IN" && (
              <button className="btn btn-success btn-sm" disabled={checkOut.isPending} onClick={() => checkOut.mutate()}>
                Check Out
              </button>
            )}
          </div>
        }
      />

      <div className="p-4" style={{ maxWidth: 760 }}>
        {isLoading && <div className="text-muted">Loading…</div>}
        {r && (
          <>
            <div className="card mb-3">
              <div className="card-body d-flex justify-content-between align-items-center">
                <div>
                  <div className="text-muted text-uppercase" style={{ fontSize: 10, letterSpacing: 1.5 }}>
                    Reservation No.
                  </div>
                  <div className="fw-bold">{r.reservationNumber}</div>
                  <div className="mt-1"><StatusBadge status={r.status} /></div>
                </div>
                <div className="text-end">
                  <div className="text-muted small">Created</div>
                  <div>{formatDateTime(r.createdAt)}</div>
                </div>
              </div>
            </div>

            <div className="card mb-3">
              <div className="card-header">Stay Details</div>
              <div className="card-body">
                <div className="row g-3">
                  <Field label="Guest"><span className="fw-semibold">{r.guestFullName || r.guestId}</span></Field>
                  <Field label="Room"><span className="fw-semibold">{r.roomNumber || r.roomId}</span></Field>
                  <Field label="Check-In">{formatDate(r.checkInDate)}</Field>
                  <Field label="Check-Out">{formatDate(r.checkOutDate)}</Field>
                  <Field label="Nights">{formatNights(r.nights)}</Field>
                  <Field label="Guests">{guests.join(", ")}</Field>
                </div>
              </div>
            </div>

            <div className="card mb-3">
              <div className="card-header">Pricing</div>
              <div className="card-body">
                <div className="row g-3">
                  <Field label="Rate / Night">{formatRp(r.ratePerNight)}</Field>
                  <Field label="Subtotal"><span className="fw-bold">{formatRp(r.subtotal)}</span></Field>
                </div>
              </div>
            </div>

            {r.specialRequests && (
              <div className="card mb-3">
                <div className="card-header">Special Requests</div>
                <div className="card-body">
                  <p className="mb-0 text-muted fst-italic">{r.specialRequests}</p>
                </div>
              </div>
            )}
          </>
        )}
      </div>

      {confirmCancel && r && (
        <>
          <div className="modal d-block" tabIndex={-1}>
            <div className="modal-dialog">
              <div className="modal-content">
                <div className="modal-header">
                  <h5 className="modal-title">Cancel Reservation</h5>
                  <button type="button" className="btn-close" onClick={() => setConfirmCancel(false)} />
                </div>
                <div className="modal-body">
                  <p className="mb-0">
                    Are you sure you want to cancel reservation <strong>{r.reservationNumber}</strong>?
                    This action cannot be undone.
                  </p>
                </div>
                <div className="modal-footer">
                  <button className="btn btn-outline-secondary" onClick={() => setConfirmCancel(false)}>
                    Keep Reservation
                  </button>
                  <button className="btn btn-danger" disabled={cancel.isPending} onClick={() => cancel.mutate()}>
                    Cancel Reservation
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
