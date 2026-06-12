import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { api, ApiError } from "../lib/api";

export default function HotelForm() {
  const navigate = useNavigate();
  const [name, setName] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;
    setError(null);
    setBusy(true);
    try {
      const hotel = await api.hotels.create(name.trim());
      navigate(`/group/hotels/${hotel.id}/setup`);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to create hotel");
      setBusy(false);
    }
  };

  return (
    <div>
      <h2 className="h4 mb-1">New Hotel</h2>
      <p className="text-muted small mb-4">
        Create a hotel under your group. It starts in <strong>SETUP</strong> status — you'll complete the
        profile, policy, rooms, and staff in the setup wizard.
      </p>

      <div className="card" style={{ maxWidth: 520 }}>
        <div className="card-body">
          <form onSubmit={onSubmit}>
            <div className="mb-4">
              <label className="form-label fw-semibold">Hotel Name</label>
              <input
                className="form-control"
                maxLength={200}
                placeholder="e.g. The Grand Nusantara"
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
              />
            </div>
            {error && <div className="alert alert-danger py-2 small">{error}</div>}
            <div className="d-flex gap-2">
              <button type="submit" className="btn btn-primary" disabled={busy}>
                {busy ? "Creating…" : "Create Hotel"}
              </button>
              <Link to="/group/hotels" className="btn btn-outline-secondary">Cancel</Link>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
