import { useMemo, useState, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { api, ApiError } from "../lib/api";
import { useAuth } from "../lib/auth";
import { addRecentHotel, getRecentHotels, removeRecentHotel } from "../lib/session";

export default function Login() {
  const { setSession } = useAuth();
  const navigate = useNavigate();

  const [recents, setRecents] = useState(() => getRecentHotels());
  // "pick" lists saved hotels; "manual" shows the name + ID fields for a new one.
  const [mode, setMode] = useState<"pick" | "manual">(
    () => (getRecentHotels().length ? "pick" : "manual"),
  );
  const [selectedId, setSelectedId] = useState(() => getRecentHotels()[0]?.id ?? "");
  const [hotelName, setHotelName] = useState("");
  const [hotelId, setHotelId] = useState("");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const selectedName = useMemo(
    () => recents.find((h) => h.id === selectedId)?.name ?? "",
    [recents, selectedId],
  );

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);

    const effectiveId = (mode === "pick" ? selectedId : hotelId).trim();
    const effectiveName = (mode === "pick" ? selectedName : hotelName).trim();
    if (!effectiveId) {
      setError("Please choose or enter a hotel.");
      return;
    }

    setBusy(true);
    try {
      const res = await api.auth.login(username, password, effectiveId);
      // Only remember the hotel once credentials are accepted.
      addRecentHotel({ id: effectiveId, name: effectiveName });
      setSession({ ...res, role: res.role });
      navigate("/dashboard", { replace: true });
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Login failed");
    } finally {
      setBusy(false);
    }
  };

  const forgetHotel = (id: string) => {
    removeRecentHotel(id);
    const next = getRecentHotels();
    setRecents(next);
    if (id === selectedId) setSelectedId(next[0]?.id ?? "");
    if (next.length === 0) setMode("manual");
  };

  return (
    <div className="d-flex align-items-center justify-content-center bg-light" style={{ minHeight: "100vh" }}>
      <div className="card shadow-sm" style={{ width: 380 }}>
        <div className="card-body p-4">
          <h4 className="fw-bold mb-1">HospitOps</h4>
          <p className="text-muted small mb-4">Hotel staff sign in</p>
          <form onSubmit={onSubmit}>
            {mode === "pick" ? (
              <div className="mb-3">
                <label className="form-label">Hotel</label>
                <select
                  className="form-select"
                  value={selectedId}
                  onChange={(e) => setSelectedId(e.target.value)}
                >
                  {recents.map((h) => (
                    <option key={h.id} value={h.id}>{h.name}</option>
                  ))}
                </select>
                <div className="d-flex justify-content-between mt-1">
                  <button
                    type="button"
                    className="btn btn-link btn-sm p-0 small text-decoration-none"
                    onClick={() => { setMode("manual"); setHotelName(""); setHotelId(""); }}
                  >
                    + Use a different hotel
                  </button>
                  {selectedId && (
                    <button
                      type="button"
                      className="btn btn-link btn-sm p-0 small text-muted text-decoration-none"
                      onClick={() => forgetHotel(selectedId)}
                    >
                      Forget
                    </button>
                  )}
                </div>
              </div>
            ) : (
              <>
                <div className="mb-3">
                  <label className="form-label">Hotel name</label>
                  <input
                    className="form-control"
                    value={hotelName}
                    onChange={(e) => setHotelName(e.target.value)}
                    placeholder="e.g. Grand Sari Bali"
                  />
                  <div className="form-text">Shown on this device so you can pick it next time.</div>
                </div>
                <div className="mb-3">
                  <label className="form-label">Hotel ID</label>
                  <input
                    className="form-control"
                    value={hotelId}
                    onChange={(e) => setHotelId(e.target.value)}
                    placeholder="hotel UUID"
                    required
                  />
                  {recents.length > 0 && (
                    <button
                      type="button"
                      className="btn btn-link btn-sm p-0 small text-decoration-none mt-1"
                      onClick={() => setMode("pick")}
                    >
                      ← Back to saved hotels
                    </button>
                  )}
                </div>
              </>
            )}
            <div className="mb-3">
              <label className="form-label">Username</label>
              <input
                className="form-control"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
              />
            </div>
            <div className="mb-3">
              <label className="form-label">Password</label>
              <input
                type="password"
                className="form-control"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </div>
            {error && <div className="alert alert-danger py-2 small">{error}</div>}
            <button className="btn btn-primary w-100" disabled={busy}>
              {busy ? "Signing in…" : "Sign in"}
            </button>
          </form>
          <div className="text-center mt-3">
            <a href="/group/login" className="small">Group admin login</a>
          </div>
        </div>
      </div>
    </div>
  );
}
