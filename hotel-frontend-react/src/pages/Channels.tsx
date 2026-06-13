import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api, ApiError } from "../lib/api";
import { useToast } from "../lib/toast";
import { formatDateTime } from "../lib/utils";
import PageHeader from "../components/PageHeader";
import StatusBadge from "../components/StatusBadge";
import type { ChannelRoomTypeMapping, RoomType } from "../lib/types";

interface MapForm {
  roomTypeId: string;
  roomTypeName: string;
  externalRoomTypeId: string;
  externalRatePlanId: string;
}

export default function Channels() {
  const toast = useToast();
  const qc = useQueryClient();
  const [propInput, setPropInput] = useState("");
  const [mapForm, setMapForm] = useState<MapForm | null>(null);

  const propertyQ = useQuery({
    queryKey: ["channel", "property"],
    queryFn: () => api.channel.getProperty(),
    retry: false,
  });
  const notConfigured = propertyQ.error instanceof ApiError && propertyQ.error.status === 404;
  const property = propertyQ.data;

  const roomTypesQ = useQuery({
    queryKey: ["room-types", { size: 100 }],
    queryFn: () => api.roomTypes.list({ size: 100 }),
  });
  const mappingsQ = useQuery({
    queryKey: ["channel", "room-types"],
    queryFn: () => api.channel.listRoomTypeMappings(),
  });
  const syncQ = useQuery({
    queryKey: ["channel", "sync-messages"],
    queryFn: () => api.channel.syncMessages(20),
    refetchInterval: 10000,
  });
  const inboundQ = useQuery({
    queryKey: ["channel", "inbound"],
    queryFn: () => api.channel.inbound(20),
    refetchInterval: 10000,
  });

  const onError = (e: unknown) => toast(e instanceof ApiError ? e.message : "Request failed", "danger");
  const refetchProperty = () => qc.invalidateQueries({ queryKey: ["channel", "property"] });

  const configure = useMutation({
    mutationFn: (id: string) => api.channel.configureProperty(id),
    onSuccess: () => { toast("Channel property saved"); setPropInput(""); void refetchProperty(); },
    onError,
  });
  const toggle = useMutation({
    mutationFn: (enabled: boolean) => (enabled ? api.channel.disable() : api.channel.enable()),
    onSuccess: () => { toast("Channel updated"); void refetchProperty(); },
    onError,
  });
  const saveMapping = useMutation({
    mutationFn: (f: MapForm) => api.channel.mapRoomType(f.roomTypeId, f.externalRoomTypeId, f.externalRatePlanId),
    onSuccess: () => {
      toast("Room type mapped");
      setMapForm(null);
      void qc.invalidateQueries({ queryKey: ["channel", "room-types"] });
    },
    onError,
  });

  const roomTypes: RoomType[] = roomTypesQ.data?.content ?? [];
  const mappingByRt = new Map<string, ChannelRoomTypeMapping>(
    (mappingsQ.data ?? []).map((m) => [m.roomTypeId, m]),
  );

  const openMap = (rt: RoomType) => {
    const m = mappingByRt.get(rt.id);
    setMapForm({
      roomTypeId: rt.id,
      roomTypeName: rt.name,
      externalRoomTypeId: m?.externalRoomTypeId ?? "",
      externalRatePlanId: m?.externalRatePlanId ?? "",
    });
  };

  return (
    <div>
      <PageHeader
        title="Channel Manager"
        action={
          property ? (
            <button
              className={`btn btn-sm ${property.enabled ? "btn-outline-danger" : "btn-success"}`}
              disabled={toggle.isPending}
              onClick={() => toggle.mutate(property.enabled)}
            >
              {property.enabled ? "Disable sync" : "Enable sync"}
            </button>
          ) : undefined
        }
      />

      <div className="p-4 d-flex flex-column gap-4">
        {/* ── Property hookup ──────────────────────────────── */}
        <div className="card">
          <div className="card-header fw-semibold">Provider connection</div>
          <div className="card-body">
            {notConfigured && (
              <div className="row g-2 align-items-end" style={{ maxWidth: 640 }}>
                <div className="col">
                  <label className="form-label small text-muted">Channex property ID</label>
                  <input
                    className="form-control"
                    placeholder="e.g. 3cc037a5-a0b6-4150-acbd-…"
                    value={propInput}
                    onChange={(e) => setPropInput(e.target.value)}
                  />
                </div>
                <div className="col-auto">
                  <button
                    className="btn btn-primary"
                    disabled={!propInput.trim() || configure.isPending}
                    onClick={() => configure.mutate(propInput.trim())}
                  >
                    Connect
                  </button>
                </div>
              </div>
            )}
            {property && (
              <div className="d-flex flex-wrap gap-4 align-items-center">
                <div>
                  <div className="small text-muted">Provider</div>
                  <div className="fw-semibold">{property.provider}</div>
                </div>
                <div>
                  <div className="small text-muted">Property ID</div>
                  <code>{property.externalPropertyId}</code>
                </div>
                <div>
                  <div className="small text-muted">Status</div>
                  <span className={`badge bg-${property.enabled ? "success" : "secondary"}`}>
                    {property.enabled ? "SYNC ON" : "SYNC OFF"}
                  </span>
                </div>
              </div>
            )}
            {property && (
              <div className="row g-2 align-items-end mt-3" style={{ maxWidth: 640 }}>
                <div className="col">
                  <label className="form-label small text-muted">Change property ID</label>
                  <input
                    className="form-control form-control-sm"
                    placeholder={property.externalPropertyId}
                    value={propInput}
                    onChange={(e) => setPropInput(e.target.value)}
                  />
                </div>
                <div className="col-auto">
                  <button
                    className="btn btn-outline-primary btn-sm"
                    disabled={!propInput.trim() || configure.isPending}
                    onClick={() => configure.mutate(propInput.trim())}
                  >
                    Update
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* ── Room-type mappings ───────────────────────────── */}
        <div className="card">
          <div className="card-header fw-semibold">Room-type mappings</div>
          <div className="table-responsive">
            <table className="table table-hover mb-0 align-middle">
              <thead>
                <tr><th>Room type</th><th>Channex room type</th><th>Rate plan</th><th></th></tr>
              </thead>
              <tbody>
                {roomTypes.map((rt) => {
                  const m = mappingByRt.get(rt.id);
                  return (
                    <tr key={rt.id}>
                      <td><strong>{rt.name}</strong></td>
                      <td>{m ? <code>{m.externalRoomTypeId}</code> : <span className="text-muted">—</span>}</td>
                      <td>{m ? <code>{m.externalRatePlanId}</code> : <span className="text-muted">—</span>}</td>
                      <td className="text-end">
                        <button className="btn btn-outline-secondary btn-sm" onClick={() => openMap(rt)}>
                          {m ? "Edit" : "Map"}
                        </button>
                      </td>
                    </tr>
                  );
                })}
                {roomTypes.length === 0 && (
                  <tr><td colSpan={4} className="text-center text-muted py-4">No room types yet</td></tr>
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* ── Outbound sync ────────────────────────────────── */}
        <div className="card">
          <div className="card-header fw-semibold">Outbound sync (latest)</div>
          <div className="table-responsive">
            <table className="table table-sm mb-0 align-middle">
              <thead>
                <tr><th>Type</th><th>Status</th><th>Attempts</th><th>Next attempt</th><th>Last error</th><th>Created</th></tr>
              </thead>
              <tbody>
                {(syncQ.data ?? []).map((m) => (
                  <tr key={m.id}>
                    <td>{m.type}</td>
                    <td><StatusBadge status={m.status} /></td>
                    <td>{m.attempts}</td>
                    <td className="small text-muted">{formatDateTime(m.nextAttemptAt)}</td>
                    <td className="small text-danger" style={{ maxWidth: 280 }}>{m.lastError ?? ""}</td>
                    <td className="small text-muted">{formatDateTime(m.createdAt)}</td>
                  </tr>
                ))}
                {(syncQ.data?.length ?? 0) === 0 && (
                  <tr><td colSpan={6} className="text-center text-muted py-4">No sync messages yet</td></tr>
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* ── Inbound OTA bookings ─────────────────────────── */}
        <div className="card">
          <div className="card-header fw-semibold">Inbound OTA bookings (latest)</div>
          <div className="table-responsive">
            <table className="table table-sm mb-0 align-middle">
              <thead>
                <tr><th>OTA</th><th>Booking</th><th>Status</th><th>Reservation</th><th>Updated</th></tr>
              </thead>
              <tbody>
                {(inboundQ.data ?? []).map((b) => (
                  <tr key={b.externalBookingId}>
                    <td>{b.otaName ?? "—"}{b.otaReservationCode ? ` · ${b.otaReservationCode}` : ""}</td>
                    <td className="small"><code>{b.externalBookingId}</code></td>
                    <td><StatusBadge status={b.status} /></td>
                    <td className="small text-muted">{b.reservationId ?? "—"}</td>
                    <td className="small text-muted">{formatDateTime(b.updatedAt)}</td>
                  </tr>
                ))}
                {(inboundQ.data?.length ?? 0) === 0 && (
                  <tr><td colSpan={5} className="text-center text-muted py-4">No inbound bookings yet</td></tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {/* ── Map room type modal ───────────────────────────── */}
      {mapForm && (
        <>
          <div className="modal d-block" tabIndex={-1}>
            <div className="modal-dialog">
              <div className="modal-content">
                <div className="modal-header">
                  <h5 className="modal-title">Map “{mapForm.roomTypeName}”</h5>
                  <button type="button" className="btn-close" onClick={() => setMapForm(null)} />
                </div>
                <div className="modal-body">
                  <div className="mb-3">
                    <label className="form-label">Channex room type ID *</label>
                    <input
                      className="form-control"
                      value={mapForm.externalRoomTypeId}
                      onChange={(e) => setMapForm({ ...mapForm, externalRoomTypeId: e.target.value })}
                    />
                  </div>
                  <div className="mb-3">
                    <label className="form-label">Channex rate plan ID *</label>
                    <input
                      className="form-control"
                      value={mapForm.externalRatePlanId}
                      onChange={(e) => setMapForm({ ...mapForm, externalRatePlanId: e.target.value })}
                    />
                  </div>
                </div>
                <div className="modal-footer">
                  <button className="btn btn-outline-secondary" onClick={() => setMapForm(null)}>Cancel</button>
                  <button
                    className="btn btn-primary"
                    disabled={!mapForm.externalRoomTypeId.trim() || !mapForm.externalRatePlanId.trim() || saveMapping.isPending}
                    onClick={() => saveMapping.mutate(mapForm)}
                  >
                    Save
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
