import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useMutation, useQuery } from "@tanstack/react-query";
import { api, ApiError } from "../lib/api";
import { useAuth } from "../lib/auth";
import { useToast } from "../lib/toast";
import { formatRp } from "../lib/utils";
import type { HotelSummary } from "../lib/types";

function Kpi({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="col-6 border-top p-3">
      <div className="text-muted text-uppercase" style={{ fontSize: 10, letterSpacing: 1.5 }}>{label}</div>
      <div className="fs-5 fw-bold">{value}</div>
    </div>
  );
}

function HotelCard({ s, onEnter }: { s: HotelSummary; onEnter: (s: HotelSummary) => void }) {
  const pct = s.totalRooms ? Math.round((s.occupiedRooms / s.totalRooms) * 100) : 0;
  const name = s.hotelName || s.hotelId;
  const isActive = s.hotelStatus === "ACTIVE";
  const badgeColor = isActive ? "success" : s.hotelStatus === "SETUP" ? "warning" : "secondary";

  return (
    <div className="col-12 col-md-6 col-xl-4">
      <div className="card h-100">
        <div className="p-4 pb-3">
          <div className="d-flex justify-content-between align-items-start">
            <div className="fw-semibold d-flex align-items-center gap-2">
              {name}
              <span className={`badge bg-${badgeColor}`} style={{ fontSize: 9 }}>{s.hotelStatus}</span>
            </div>
            {isActive ? (
              <button className="btn btn-primary btn-sm" onClick={() => onEnter(s)}>Enter →</button>
            ) : (
              <span className="badge bg-light text-muted">{s.hotelStatus}</span>
            )}
          </div>

          {isActive && (
            <div className="mt-3">
              <div className="d-flex justify-content-between mb-1">
                <span className="text-muted" style={{ fontSize: 11 }}>Occupancy</span>
                <span className="fw-semibold" style={{ fontSize: 12 }}>{pct}%</span>
              </div>
              <div className="progress" style={{ height: 4 }}>
                <div className="progress-bar" style={{ width: `${pct}%` }} />
              </div>
              <div className="text-muted mt-1" style={{ fontSize: 11 }}>
                {s.occupiedRooms} / {s.totalRooms} rooms occupied
              </div>
            </div>
          )}
        </div>

        {isActive ? (
          <div className="row g-0">
            <Kpi label="Arrivals Today" value={s.arrivalsToday} />
            <Kpi label="Departures Today" value={s.departuresToday} />
            <Kpi label="Revenue Today" value={formatRp(s.revenueToday)} />
            <Kpi label="Revenue This Month" value={formatRp(s.revenueMonth)} />
          </div>
        ) : (
          <div className="p-3 border-top text-muted small">
            {s.hotelStatus === "SETUP"
              ? "Hotel setup is not complete. KPIs appear once the hotel is active."
              : "This hotel is suspended."}
          </div>
        )}
      </div>
    </div>
  );
}

export default function GroupDashboard() {
  const { setSession } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();
  const [pending, setPending] = useState<HotelSummary | null>(null);

  const { data, isLoading, error } = useQuery({
    queryKey: ["group-dashboard"],
    queryFn: () => api.groupDashboard.get(),
  });

  const enter = useMutation({
    mutationFn: (s: HotelSummary) => api.groupAuth.enterHotel(s.hotelId),
    onSuccess: (res) => {
      setSession({ ...res, role: "GROUP_ADMIN" });
      navigate("/dashboard");
    },
    onError: (e) => {
      setPending(null);
      toast(e instanceof ApiError ? e.message : "Failed to enter hotel", "danger");
    },
  });

  const hotels = data ?? [];

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h2 className="h4 mb-0">Portfolio Overview</h2>
          <p className="text-muted small mb-0">
            {hotels.length} hotel{hotels.length !== 1 ? "s" : ""} · live summary
          </p>
        </div>
      </div>

      {isLoading && <div className="text-muted">Loading hotels…</div>}
      {error && <div className="alert alert-danger">{error instanceof ApiError ? error.message : "Failed to load hotels."}</div>}

      {!isLoading && hotels.length === 0 && (
        <div className="text-center text-muted py-5">No hotels found in this group.</div>
      )}

      <div className="row g-4">
        {hotels.map((s) => (
          <HotelCard key={s.hotelId} s={s} onEnter={setPending} />
        ))}
      </div>

      {pending && (
        <>
          <div className="modal d-block" tabIndex={-1}>
            <div className="modal-dialog modal-dialog-centered">
              <div className="modal-content">
                <div className="modal-header">
                  <h5 className="modal-title">Enter Hotel</h5>
                  <button type="button" className="btn-close" onClick={() => setPending(null)} />
                </div>
                <div className="modal-body">
                  <p className="mb-0">
                    You are about to switch into <strong>{pending.hotelName || pending.hotelId}</strong>.
                    Your group token will be exchanged for a hotel-scoped session.
                  </p>
                </div>
                <div className="modal-footer">
                  <button className="btn btn-outline-secondary btn-sm" onClick={() => setPending(null)}>Cancel</button>
                  <button
                    className="btn btn-primary btn-sm"
                    disabled={enter.isPending}
                    onClick={() => enter.mutate(pending)}
                  >
                    Enter Hotel
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
