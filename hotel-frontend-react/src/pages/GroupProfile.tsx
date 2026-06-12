import { Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { api, ApiError } from "../lib/api";
import { formatDate } from "../lib/utils";

function Row({ label, children, mono }: { label: string; children: React.ReactNode; mono?: boolean }) {
  return (
    <div className="py-3 border-bottom">
      <div className="text-muted text-uppercase" style={{ fontSize: 10, letterSpacing: 1.5 }}>{label}</div>
      <div className={mono ? "font-monospace small" : "fw-medium"}>{children}</div>
    </div>
  );
}

export default function GroupProfile() {
  const { data, isLoading, error } = useQuery({
    queryKey: ["group-profile"],
    queryFn: () => api.group.profile(),
  });

  return (
    <div>
      <h2 className="h4 mb-1">Group Profile</h2>
      <p className="text-muted small mb-4">Your hotel group account details.</p>

      {isLoading && <div className="text-muted">Loading profile…</div>}
      {error && <div className="alert alert-danger">{error instanceof ApiError ? error.message : "Failed to load profile."}</div>}

      {data && (
        <>
          <div className="card" style={{ maxWidth: 520 }}>
            <div className="card-body">
              <Row label="Group Name">{data.name || "—"}</Row>
              <Row label="Owner / Admin Email">{data.ownerEmail || "—"}</Row>
              <Row label="Group ID" mono>{data.id || "—"}</Row>
              <Row label="Member Since">{data.createdAt ? formatDate(data.createdAt) : "—"}</Row>
            </div>
          </div>
          <div className="mt-4 d-flex gap-2">
            <Link to="/group/hotels" className="btn btn-primary btn-sm">Manage Hotels</Link>
            <Link to="/group/dashboard" className="btn btn-outline-secondary btn-sm">Dashboard</Link>
          </div>
        </>
      )}
    </div>
  );
}
