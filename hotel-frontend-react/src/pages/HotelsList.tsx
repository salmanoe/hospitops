import { useState } from "react";
import { Link } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api, ApiError } from "../lib/api";
import { useToast } from "../lib/toast";
import type { Hotel } from "../lib/types";

const STATUS_BADGE: Record<string, string> = {
  ACTIVE: "bg-success",
  SETUP: "bg-warning text-dark",
  SUSPENDED: "bg-secondary",
};

export default function HotelsList() {
  const toast = useToast();
  const qc = useQueryClient();
  const [toDelete, setToDelete] = useState<Hotel | null>(null);

  const { data, isLoading, error } = useQuery({
    queryKey: ["hotels"],
    queryFn: () => api.hotels.list(),
  });

  const del = useMutation({
    mutationFn: (h: Hotel) => api.hotels.delete(h.id),
    onSuccess: () => {
      toast("Hotel deleted.");
      setToDelete(null);
      void qc.invalidateQueries({ queryKey: ["hotels"] });
    },
    onError: (e) => { setToDelete(null); toast(e instanceof ApiError ? e.message : "Delete failed", "danger"); },
  });

  const hotels = data ?? [];

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h2 className="h4 mb-0">Hotels</h2>
          <p className="text-muted small mb-0">
            {hotels.length ? `${hotels.length} hotel${hotels.length !== 1 ? "s" : ""}` : "Manage your hotel portfolio"}
          </p>
        </div>
        <Link to="/group/hotels/new" className="btn btn-primary btn-sm">+ New Hotel</Link>
      </div>

      {isLoading && <div className="text-muted">Loading hotels…</div>}
      {error && <div className="alert alert-danger">{error instanceof ApiError ? error.message : "Failed to load hotels."}</div>}
      {!isLoading && hotels.length === 0 && (
        <div className="text-center text-muted py-5">
          <p className="mb-2">No hotels yet.</p>
          <Link to="/group/hotels/new" className="btn btn-primary btn-sm">Create your first hotel</Link>
        </div>
      )}

      <div className="d-flex flex-column gap-3">
        {hotels.map((h) => {
          const isSetup = h.status === "SETUP";
          const remaining = h.remainingSetupSteps?.length ?? 0;
          return (
            <div key={h.id} className="card">
              <div className="card-body d-flex align-items-center gap-3 flex-wrap">
                <div className="flex-grow-1">
                  <div className="fw-semibold">{h.name}</div>
                  <div className="text-muted small">
                    {h.timezone || "—"} · {h.currency || "—"} · {h.starRating ? `${h.starRating}★` : "—"}
                  </div>
                </div>
                <span className={`badge ${STATUS_BADGE[h.status] ?? "bg-secondary"}`}>{h.status}</span>
                <div className="d-flex gap-2">
                  {isSetup ? (
                    <>
                      <Link to={`/group/hotels/${h.id}/setup`} className="btn btn-outline-warning btn-sm">
                        Setup wizard ({remaining} step{remaining !== 1 ? "s" : ""} left)
                      </Link>
                      <button className="btn btn-outline-danger btn-sm" onClick={() => setToDelete(h)}>Delete</button>
                    </>
                  ) : (
                    <Link to={`/group/hotels/${h.id}/policy`} className="btn btn-outline-secondary btn-sm">Policy</Link>
                  )}
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {toDelete && (
        <>
          <div className="modal d-block" tabIndex={-1}>
            <div className="modal-dialog modal-dialog-centered">
              <div className="modal-content">
                <div className="modal-header">
                  <h5 className="modal-title text-danger">Delete Hotel</h5>
                  <button type="button" className="btn-close" onClick={() => setToDelete(null)} />
                </div>
                <div className="modal-body">
                  <p>Are you sure you want to delete <strong>{toDelete.name}</strong>?</p>
                  <p className="text-muted small mb-0">
                    This is permanent. Only hotels still in SETUP status can be deleted.
                  </p>
                </div>
                <div className="modal-footer">
                  <button className="btn btn-outline-secondary btn-sm" onClick={() => setToDelete(null)}>Cancel</button>
                  <button className="btn btn-danger btn-sm" disabled={del.isPending} onClick={() => del.mutate(toDelete)}>
                    Delete Hotel
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
