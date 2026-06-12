import { useEffect, useState, type FormEvent } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { api, ApiError } from "../lib/api";
import { useToast } from "../lib/toast";
import { formatRp } from "../lib/utils";
import PageHeader from "../components/PageHeader";
import type { RoomStatus } from "../lib/types";

export default function RoomForm() {
  const { id } = useParams();
  const isEdit = !!id;
  const navigate = useNavigate();
  const toast = useToast();

  const [roomNumber, setRoomNumber] = useState("");
  const [floor, setFloor] = useState("");
  const [roomTypeId, setRoomTypeId] = useState("");
  const [status, setStatus] = useState<RoomStatus>("AVAILABLE");
  const [notes, setNotes] = useState("");
  const [busy, setBusy] = useState(false);

  const types = useQuery({
    queryKey: ["room-types", { size: 100 }],
    queryFn: () => api.roomTypes.list({ size: 100 }),
  });

  const existing = useQuery({
    queryKey: ["room", id],
    queryFn: () => api.rooms.get(id!),
    enabled: isEdit,
  });

  useEffect(() => {
    if (existing.data) {
      const r = existing.data;
      setRoomNumber(r.roomNumber);
      setFloor(r.floor ?? "");
      setRoomTypeId(r.roomTypeId ?? "");
      setStatus(r.status);
      setNotes(r.notes ?? "");
    }
  }, [existing.data]);

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setBusy(true);
    try {
      if (isEdit) {
        await api.rooms.update(id!, { floor: Number(floor), notes: notes || null });
        toast("Room updated");
      } else {
        await api.rooms.create({ roomNumber, floor: Number(floor), roomTypeId, notes: notes || null });
        toast("Room added");
      }
      navigate("/rooms");
    } catch (err) {
      toast(err instanceof ApiError ? err.message : "Save failed", "danger");
      setBusy(false);
    }
  };

  return (
    <div>
      <PageHeader
        title={isEdit ? "Edit Room" : "Add Room"}
        action={<Link to="/rooms" className="btn btn-outline-secondary btn-sm">← Back</Link>}
      />
      <div className="p-4" style={{ maxWidth: 560 }}>
        <form onSubmit={onSubmit}>
          <div className="card mb-3">
            <div className="card-header">Room Details</div>
            <div className="card-body">
              <div className="row g-3">
                <div className="col-md-6">
                  <label className="form-label">Room Number *</label>
                  <input
                    className="form-control"
                    placeholder="e.g. 101, 204A"
                    value={roomNumber}
                    onChange={(e) => setRoomNumber(e.target.value)}
                    readOnly={isEdit}
                    required
                  />
                </div>
                <div className="col-md-6">
                  <label className="form-label">Floor *</label>
                  <input
                    type="number"
                    className="form-control"
                    min={1}
                    max={99}
                    value={floor}
                    onChange={(e) => setFloor(e.target.value)}
                    required
                  />
                </div>
                <div className="col-12">
                  <label className="form-label">Room Type *</label>
                  <select
                    className="form-select"
                    value={roomTypeId}
                    onChange={(e) => setRoomTypeId(e.target.value)}
                    disabled={isEdit}
                    required
                  >
                    <option value="">— Select type —</option>
                    {(types.data?.content ?? []).map((rt) => (
                      <option key={rt.id} value={rt.id}>
                        {rt.name} — {formatRp(rt.basePrice)}/night
                      </option>
                    ))}
                  </select>
                </div>
                {isEdit && (
                  <div className="col-12">
                    <label className="form-label">Status</label>
                    <input className="form-control" value={status.replace(/_/g, " ")} readOnly />
                    <div className="form-text">Change room status from the Housekeeping board.</div>
                  </div>
                )}
                <div className="col-12">
                  <label className="form-label">Notes</label>
                  <textarea
                    className="form-control"
                    rows={2}
                    placeholder="e.g. connecting room, corner unit…"
                    value={notes}
                    onChange={(e) => setNotes(e.target.value)}
                  />
                </div>
              </div>
            </div>
          </div>

          <div className="d-flex gap-2 justify-content-end">
            <Link to="/rooms" className="btn btn-outline-secondary">Cancel</Link>
            <button type="submit" className="btn btn-primary" disabled={busy}>
              {busy ? "Saving…" : "Save Room"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
