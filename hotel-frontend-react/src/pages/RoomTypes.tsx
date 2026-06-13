import { useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api, ApiError } from "../lib/api";
import { useToast } from "../lib/toast";
import { formatRp } from "../lib/utils";
import PageHeader from "../components/PageHeader";
import type { RoomType } from "../lib/types";

interface TypeForm {
  id: string | null;
  name: string;
  capacity: number;
  basePrice: number;
  description: string;
}

const EMPTY: TypeForm = { id: null, name: "", capacity: 2, basePrice: 0, description: "" };

export default function RoomTypes() {
  const toast = useToast();
  const qc = useQueryClient();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const setupId = params.get("setup");
  const [form, setForm] = useState<TypeForm | null>(null);

  const { data } = useQuery({
    queryKey: ["room-types", { size: 100 }],
    queryFn: () => api.roomTypes.list({ size: 100 }),
  });

  const save = useMutation({
    mutationFn: (f: TypeForm) => {
      const payload = {
        name: f.name,
        capacity: f.capacity,
        basePrice: f.basePrice,
        description: f.description || null,
      };
      return f.id ? api.roomTypes.update(f.id, payload) : api.roomTypes.create(payload);
    },
    onSuccess: async (_res, f) => {
      setForm(null);
      // In the setup wizard, creating the first room type completes ROOM_TYPE.
      if (!f.id && setupId) {
        try { await api.hotels.completeSetupStep(setupId, "ROOM_TYPE"); } catch { /* step may already be done */ }
        toast("Room type added — continuing setup…");
        navigate(`/group/hotels/${setupId}/setup`);
        return;
      }
      toast(f.id ? "Room type updated" : "Room type created");
      void qc.invalidateQueries({ queryKey: ["room-types", { size: 100 }] });
    },
    onError: (e) => toast(e instanceof ApiError ? e.message : "Save failed", "danger"),
  });

  const openEdit = (rt: RoomType) =>
    setForm({
      id: rt.id,
      name: rt.name,
      capacity: rt.capacity,
      basePrice: rt.basePrice,
      description: rt.description ?? "",
    });

  const rows = data?.content ?? [];

  return (
    <div>
      <PageHeader
        title="Room Types"
        action={
          <div className="d-flex gap-2">
            <Link to="/rooms" className="btn btn-outline-secondary btn-sm">← Rooms</Link>
            <button className="btn btn-primary btn-sm" onClick={() => setForm({ ...EMPTY })}>＋ Add Type</button>
          </div>
        }
      />
      <div className="p-4">
        <div className="card">
          <div className="table-responsive">
            <table className="table table-hover mb-0">
              <thead>
                <tr><th>Name</th><th>Capacity</th><th>Base Rate/Night</th><th>Description</th><th></th></tr>
              </thead>
              <tbody>
                {rows.map((rt) => (
                  <tr key={rt.id}>
                    <td><strong>{rt.name}</strong></td>
                    <td>{rt.capacity} pax</td>
                    <td>{formatRp(rt.basePrice)}</td>
                    <td className="text-muted small">{rt.description || "—"}</td>
                    <td>
                      <button className="btn btn-outline-secondary btn-sm" onClick={() => openEdit(rt)}>Edit</button>
                    </td>
                  </tr>
                ))}
                {rows.length === 0 && (
                  <tr><td colSpan={5} className="text-center text-muted py-4">No room types yet</td></tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {form && (
        <>
          <div className="modal d-block" tabIndex={-1}>
            <div className="modal-dialog">
              <div className="modal-content">
                <div className="modal-header">
                  <h5 className="modal-title">{form.id ? "Edit Room Type" : "Add Room Type"}</h5>
                  <button type="button" className="btn-close" onClick={() => setForm(null)} />
                </div>
                <div className="modal-body">
                  <div className="mb-3">
                    <label className="form-label">Name *</label>
                    <input
                      className="form-control"
                      placeholder="e.g. Deluxe"
                      value={form.name}
                      onChange={(e) => setForm({ ...form, name: e.target.value })}
                    />
                  </div>
                  <div className="mb-3">
                    <label className="form-label">Capacity *</label>
                    <input
                      type="number"
                      className="form-control"
                      min={1}
                      value={form.capacity}
                      onChange={(e) => setForm({ ...form, capacity: Number(e.target.value) })}
                    />
                  </div>
                  <div className="mb-3">
                    <label className="form-label">Base Price / Night (IDR) *</label>
                    <input
                      type="number"
                      className="form-control"
                      min={1}
                      step={10000}
                      value={form.basePrice}
                      onChange={(e) => setForm({ ...form, basePrice: Number(e.target.value) })}
                    />
                  </div>
                  <div className="mb-3">
                    <label className="form-label">Description</label>
                    <textarea
                      className="form-control"
                      rows={2}
                      value={form.description}
                      onChange={(e) => setForm({ ...form, description: e.target.value })}
                    />
                  </div>
                </div>
                <div className="modal-footer">
                  <button className="btn btn-outline-secondary" onClick={() => setForm(null)}>Cancel</button>
                  <button className="btn btn-primary" disabled={save.isPending} onClick={() => save.mutate(form)}>
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
