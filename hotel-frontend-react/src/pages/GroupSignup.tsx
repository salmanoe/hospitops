import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { api, ApiError } from "../lib/api";

export default function GroupSignup() {
  const navigate = useNavigate();
  const [groupName, setGroupName] = useState("");
  const [adminEmail, setAdminEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      await api.groupAuth.signup(groupName, adminEmail, password);
      navigate("/group/login");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Sign up failed");
      setBusy(false);
    }
  };

  return (
    <div className="d-flex align-items-center justify-content-center bg-light" style={{ minHeight: "100vh" }}>
      <div className="card shadow-sm" style={{ width: 400 }}>
        <div className="card-body p-4">
          <h4 className="fw-bold mb-1">HospitOps</h4>
          <p className="text-muted small mb-1" style={{ letterSpacing: 2 }}>GROUP ADMINISTRATION</p>
          <h6 className="mt-3 mb-1">Create Your Group Account</h6>
          <p className="text-muted small mb-4">Set up your hotel portfolio in minutes</p>
          <form onSubmit={onSubmit}>
            <div className="mb-3">
              <label className="form-label">Group Name</label>
              <input className="form-control" maxLength={200} value={groupName}
                onChange={(e) => setGroupName(e.target.value)} placeholder="e.g. Archipelago Hotels Group" required />
            </div>
            <div className="mb-3">
              <label className="form-label">Admin Email</label>
              <input type="email" className="form-control" maxLength={150} value={adminEmail}
                onChange={(e) => setAdminEmail(e.target.value)} placeholder="admin@yourgroup.com" required />
            </div>
            <div className="mb-4">
              <label className="form-label">Password</label>
              <input type="password" className="form-control" minLength={8} maxLength={100} value={password}
                onChange={(e) => setPassword(e.target.value)} placeholder="Min. 8 characters" required />
            </div>
            {error && <div className="alert alert-danger py-2 small">{error}</div>}
            <button className="btn btn-primary w-100" disabled={busy}>
              {busy ? "Creating…" : "Create Account"}
            </button>
          </form>
          <p className="text-center mt-3 mb-0 small">
            Already have an account? <Link to="/group/login">Sign in →</Link>
          </p>
        </div>
      </div>
    </div>
  );
}
