import { useEffect, useState, type FormEvent } from "react";
import { Link, useNavigate, useParams, useSearchParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { api, ApiError } from "../lib/api";
import { useToast } from "../lib/toast";
import PageHeader from "../components/PageHeader";
import type { Role } from "../lib/types";

const ROLES: { value: Role; label: string }[] = [
  { value: "FRONT_DESK", label: "Front Desk" },
  { value: "HOUSEKEEPING", label: "Housekeeping" },
  { value: "ACCOUNTANT", label: "Accountant" },
  { value: "MANAGER", label: "Manager" },
  { value: "ADMIN", label: "Admin" },
];

export default function StaffForm() {
  const { id } = useParams();
  const isEdit = !!id;
  const navigate = useNavigate();
  const toast = useToast();
  const [params] = useSearchParams();
  const setupId = params.get("setup");

  const [fullName, setFullName] = useState("");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [role, setRole] = useState<Role | "">("");
  const [busy, setBusy] = useState(false);

  const existing = useQuery({
    queryKey: ["staff", id],
    queryFn: () => api.staff.get(id!),
    enabled: isEdit,
  });

  useEffect(() => {
    if (existing.data) {
      setFullName(existing.data.fullName);
      setUsername(existing.data.username);
      setRole(existing.data.role);
    }
  }, [existing.data]);

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setBusy(true);
    try {
      if (isEdit) {
        await api.staff.update(id!, { fullName, role });
        if (password) {
          await api.staff.changePassword(id!, { currentPassword: "", newPassword: password });
        }
        toast("Staff updated");
      } else {
        await api.staff.create({ fullName, username, password, role });
        if (setupId) {
          try { await api.hotels.completeSetupStep(setupId, "STAFF_ACCOUNT"); } catch { /* step may already be done */ }
          toast("Staff created — setup complete!");
          navigate(`/group/hotels/${setupId}/setup`);
          return;
        }
        toast("Staff created");
      }
      navigate("/staff");
    } catch (err) {
      toast(err instanceof ApiError ? err.message : "Save failed", "danger");
      setBusy(false);
    }
  };

  return (
    <div>
      <PageHeader
        title={isEdit ? "Edit Staff" : "Add Staff"}
        action={<Link to="/staff" className="btn btn-outline-secondary btn-sm">← Back</Link>}
      />
      <div className="p-4" style={{ maxWidth: 520 }}>
        <form onSubmit={onSubmit}>
          <div className="card mb-3">
            <div className="card-header">Staff Details</div>
            <div className="card-body">
              <div className="row g-3">
                <div className="col-12">
                  <label className="form-label">Full Name *</label>
                  <input className="form-control" value={fullName} onChange={(e) => setFullName(e.target.value)} required />
                </div>
                <div className="col-12">
                  <label className="form-label">Username *</label>
                  <input
                    className="form-control"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    readOnly={isEdit}
                    required
                  />
                  {isEdit && <div className="form-text">Username cannot be changed.</div>}
                </div>
                <div className="col-12">
                  <label className="form-label">{isEdit ? "New Password (leave blank to keep current)" : "Password *"}</label>
                  <input
                    type="password"
                    className="form-control"
                    placeholder="Minimum 8 characters"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    required={!isEdit}
                  />
                </div>
                <div className="col-12">
                  <label className="form-label">Role *</label>
                  <select className="form-select" value={role} onChange={(e) => setRole(e.target.value as Role)} required>
                    <option value="">— Select role —</option>
                    {ROLES.map((r) => <option key={r.value} value={r.value}>{r.label}</option>)}
                  </select>
                </div>
              </div>
            </div>
          </div>

          <div className="d-flex gap-2 justify-content-end">
            <Link to="/staff" className="btn btn-outline-secondary">Cancel</Link>
            <button type="submit" className="btn btn-primary" disabled={busy}>
              {busy ? "Saving…" : "Save Staff"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
