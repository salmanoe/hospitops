import { Link } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api, ApiError } from "../lib/api";
import { useToast } from "../lib/toast";
import { formatDate, statusColor, statusLabel } from "../lib/utils";
import PageHeader from "../components/PageHeader";
import type { Staff as StaffMember } from "../lib/types";

export default function Staff() {
  const toast = useToast();
  const qc = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ["staff"],
    queryFn: () => api.staff.list({ size: 100 }),
  });

  const toggle = useMutation({
    mutationFn: (s: StaffMember) => api.staff.toggle(s.id),
    onSuccess: (_res, s) => {
      toast(`Staff ${s.active ? "deactivated" : "activated"}`);
      void qc.invalidateQueries({ queryKey: ["staff"] });
    },
    onError: (e) => toast(e instanceof ApiError ? e.message : "Update failed", "danger"),
  });

  const onToggle = (s: StaffMember) => {
    if (window.confirm(`${s.active ? "Deactivate" : "Activate"} this staff member?`)) {
      toggle.mutate(s);
    }
  };

  const rows = data?.content ?? [];

  return (
    <div>
      <PageHeader
        title="Staff"
        action={<Link to="/staff/new" className="btn btn-primary btn-sm">＋ Add Staff</Link>}
      />
      <div className="p-4">
        <div className="card">
          <div className="table-responsive">
            <table className="table table-hover mb-0">
              <thead>
                <tr><th>Name</th><th>Username</th><th>Role</th><th>Status</th><th>Since</th><th></th></tr>
              </thead>
              <tbody>
                {rows.map((s) => (
                  <tr key={s.id}>
                    <td><strong>{s.fullName}</strong></td>
                    <td className="text-muted">{s.username}</td>
                    <td><span className={`badge bg-${statusColor(s.role)}`}>{statusLabel(s.role)}</span></td>
                    <td>
                      <span className={`badge ${s.active ? "bg-success" : "bg-secondary"}`}>
                        {s.active ? "Active" : "Inactive"}
                      </span>
                    </td>
                    <td>{formatDate(s.createdAt)}</td>
                    <td>
                      <div className="d-flex gap-1">
                        <Link to={`/staff/${s.id}/edit`} className="btn btn-outline-secondary btn-sm">Edit</Link>
                        <button
                          className={"btn btn-sm " + (s.active ? "btn-outline-danger" : "btn-outline-success")}
                          disabled={toggle.isPending}
                          onClick={() => onToggle(s)}
                        >
                          {s.active ? "Deactivate" : "Activate"}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
                {!isLoading && rows.length === 0 && (
                  <tr><td colSpan={6} className="text-center text-muted py-4">No staff found</td></tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
}
