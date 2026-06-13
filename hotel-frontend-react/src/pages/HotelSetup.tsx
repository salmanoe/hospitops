import { useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api, ApiError } from "../lib/api";
import { useAuth } from "../lib/auth";
import { useToast } from "../lib/toast";
import type { SetupStep } from "../lib/types";

interface StepMeta {
  key: SetupStep;
  label: string;
  title: string;
  desc: string;
  kind: "mark" | "policy" | "enter";
  dest?: string;
  linkText?: string;
}

export default function HotelSetup() {
  const { id = "" } = useParams();
  const navigate = useNavigate();
  const toast = useToast();
  const qc = useQueryClient();
  const { setSession } = useAuth();
  const [showEnter, setShowEnter] = useState(false);

  const { data: hotel, isLoading, error } = useQuery({
    queryKey: ["hotel", id],
    queryFn: () => api.hotels.get(id),
  });

  const STEPS: StepMeta[] = [
    { key: "PROFILE", label: "Profile", title: "Hotel Profile",
      desc: "Confirm the hotel name and basic details. Click \"Mark complete\" once reviewed.", kind: "mark" },
    { key: "POLICY", label: "Policy", title: "Policy Config",
      desc: "Set the tax rate and invoice branding. Saving the policy auto-completes this step.",
      kind: "policy", dest: `/group/hotels/${id}/policy?setup=1`, linkText: "Configure Policy →" },
    { key: "ROOM_TYPE", label: "Room Type", title: "Room Types",
      desc: "Add at least one room type. Creating one auto-completes this step.",
      kind: "enter", dest: `/room-types?setup=${id}`, linkText: "Add Room Type →" },
    { key: "ROOM", label: "Rooms", title: "Rooms",
      desc: "Add at least one room linked to a room type. Creating one auto-completes this step.",
      kind: "enter", dest: `/rooms/new?setup=${id}`, linkText: "Add Room →" },
    { key: "STAFF_ACCOUNT", label: "Staff", title: "First Staff Account",
      desc: "Create at least one ADMIN or MANAGER account. Creating one auto-completes this step.",
      kind: "enter", dest: `/staff/new?setup=${id}`, linkText: "Add Staff →" },
  ];

  const remaining = hotel?.remainingSetupSteps ?? [];
  const remainingSet = new Set(remaining);

  const markComplete = useMutation({
    mutationFn: (step: SetupStep) => api.hotels.completeSetupStep(id, step),
    onSuccess: () => { toast("Step marked complete."); void qc.invalidateQueries({ queryKey: ["hotel", id] }); },
    onError: (e) => toast(e instanceof ApiError ? e.message : "Failed to complete step", "danger"),
  });

  // Enter the hotel's context, then navigate to the (hotel-scoped) destination.
  const enterThenGo = useMutation({
    mutationFn: () => api.groupAuth.enterHotel(id),
    onSuccess: (res, dest: unknown) => {
      setSession({ ...res, role: "GROUP_ADMIN" });
      navigate(dest as string);
    },
    onError: (e) => toast(e instanceof ApiError ? e.message : "Failed to enter hotel session", "danger"),
  });

  const enterHotel = useMutation({
    mutationFn: () => api.groupAuth.enterHotel(id),
    onSuccess: (res) => { setSession({ ...res, role: "GROUP_ADMIN" }); navigate("/dashboard"); },
    onError: (e) => { setShowEnter(false); toast(e instanceof ApiError ? e.message : "Failed to enter hotel", "danger"); },
  });

  return (
    <div style={{ maxWidth: 760, margin: "0 auto" }}>
      {isLoading && <div className="text-muted">Loading hotel…</div>}
      {error && <div className="alert alert-danger">{error instanceof ApiError ? error.message : "Failed to load hotel."}</div>}

      {hotel && (
        <>
          <h2 className="h4 mb-1">{hotel.name}</h2>
          <p className="text-muted small mb-4">Setup wizard · {hotel.status}</p>

          {/* Stepper */}
          <div className="d-flex mb-4">
            {STEPS.map((s, i) => {
              const done = !remainingSet.has(s.key);
              const active = !done && remaining[0] === s.key;
              return (
                <div key={s.key} className="flex-fill text-center">
                  <div
                    className={
                      "rounded-circle mx-auto d-flex align-items-center justify-content-center fw-bold " +
                      (done ? "bg-success text-white" : active ? "border border-primary text-primary" : "border text-muted")
                    }
                    style={{ width: 32, height: 32 }}
                  >
                    {done ? "✓" : i + 1}
                  </div>
                  <div className={"small mt-1 " + (done ? "text-success" : active ? "fw-semibold" : "text-muted")}>
                    {s.label}
                  </div>
                </div>
              );
            })}
          </div>

          {hotel.status === "ACTIVE" && (
            <div className="alert alert-success">
              <div className="fw-semibold mb-1">✓ Hotel is ACTIVE</div>
              <div className="small text-muted mb-2">All setup steps are complete. Enter the hotel to start operations.</div>
              <button className="btn btn-primary btn-sm" onClick={() => setShowEnter(true)}>Enter Hotel →</button>
            </div>
          )}

          {/* Step cards */}
          {STEPS.map((s) => {
            const done = !remainingSet.has(s.key);
            return (
              <div key={s.key} className={"card mb-3" + (done ? " opacity-75" : "")}>
                <div className="card-body">
                  <div className="d-flex align-items-center gap-2">
                    <span className={`badge ${done ? "bg-success" : "bg-warning text-dark"}`}>
                      {done ? "✓ Done" : "Pending"}
                    </span>
                    <span className="fw-semibold">{s.title}</span>
                  </div>
                  <div className="text-muted small mt-2">{s.desc}</div>
                  {!done && (
                    <div className="mt-3">
                      {s.kind === "mark" && (
                        <button className="btn btn-primary btn-sm" disabled={markComplete.isPending}
                          onClick={() => markComplete.mutate(s.key)}>
                          Mark complete
                        </button>
                      )}
                      {s.kind === "policy" && (
                        <Link to={s.dest!} className="btn btn-outline-secondary btn-sm">{s.linkText}</Link>
                      )}
                      {s.kind === "enter" && (
                        <button className="btn btn-outline-secondary btn-sm" disabled={enterThenGo.isPending}
                          onClick={() => enterThenGo.mutate(s.dest)}>
                          {s.linkText}
                        </button>
                      )}
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </>
      )}

      {showEnter && hotel && (
        <>
          <div className="modal d-block" tabIndex={-1}>
            <div className="modal-dialog modal-dialog-centered">
              <div className="modal-content">
                <div className="modal-header">
                  <h5 className="modal-title">Enter Hotel</h5>
                  <button type="button" className="btn-close" onClick={() => setShowEnter(false)} />
                </div>
                <div className="modal-body">
                  <p className="mb-0">Switch into <strong>{hotel.name}</strong> as a hotel-scoped session?</p>
                </div>
                <div className="modal-footer">
                  <button className="btn btn-outline-secondary btn-sm" onClick={() => setShowEnter(false)}>Cancel</button>
                  <button className="btn btn-primary btn-sm" disabled={enterHotel.isPending} onClick={() => enterHotel.mutate()}>
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
