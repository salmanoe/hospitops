import { useEffect, useState, type FormEvent } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { api, ApiError } from "../lib/api";
import { useToast } from "../lib/toast";
import PageHeader from "../components/PageHeader";
import type { Guest } from "../lib/types";

type GuestForm = Pick<Guest, "fullName" | "idNumber" | "nationality" | "phone" | "email" | "address">;

const EMPTY: GuestForm = {
  fullName: "",
  idNumber: "",
  nationality: "",
  phone: "",
  email: "",
  address: "",
};

export default function GuestForm() {
  const { id } = useParams();
  const isEdit = !!id;
  const navigate = useNavigate();
  const toast = useToast();

  const [form, setForm] = useState<GuestForm>(EMPTY);
  const [busy, setBusy] = useState(false);

  const existing = useQuery({
    queryKey: ["guest", id],
    queryFn: () => api.guests.get(id!),
    enabled: isEdit,
  });

  useEffect(() => {
    if (existing.data) {
      const g = existing.data;
      setForm({
        fullName: g.fullName ?? "",
        idNumber: g.idNumber ?? "",
        nationality: g.nationality ?? "",
        phone: g.phone ?? "",
        email: g.email ?? "",
        address: g.address ?? "",
      });
    }
  }, [existing.data]);

  const set = (k: keyof GuestForm) => (e: { target: { value: string } }) =>
    setForm((f) => ({ ...f, [k]: e.target.value }));

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setBusy(true);
    // Normalise empty strings to null (mirrors Utils.formData).
    const payload: Partial<Guest> = Object.fromEntries(
      Object.entries(form).map(([k, v]) => [k, v === "" ? null : v]),
    );
    try {
      if (isEdit) await api.guests.update(id!, payload);
      else await api.guests.register(payload);
      toast(isEdit ? "Guest updated" : "Guest registered");
      navigate("/guests");
    } catch (err) {
      toast(err instanceof ApiError ? err.message : "Save failed", "danger");
      setBusy(false);
    }
  };

  return (
    <div>
      <PageHeader
        title={isEdit ? "Edit Guest" : "Register Guest"}
        action={<Link to="/guests" className="btn btn-outline-secondary btn-sm">← Back</Link>}
      />
      <div className="p-4" style={{ maxWidth: 640 }}>
        <form onSubmit={onSubmit}>
          <div className="card mb-3">
            <div className="card-header">Personal Information</div>
            <div className="card-body">
              <div className="row g-3">
                <div className="col-12">
                  <label className="form-label">Full Name *</label>
                  <input className="form-control" value={form.fullName} onChange={set("fullName")} required />
                </div>
                <div className="col-md-6">
                  <label className="form-label">ID Number (Passport / KTP)</label>
                  <input className="form-control" value={form.idNumber ?? ""} onChange={set("idNumber")} />
                </div>
                <div className="col-md-6">
                  <label className="form-label">Nationality</label>
                  <input className="form-control" value={form.nationality ?? ""} onChange={set("nationality")} />
                </div>
              </div>
            </div>
          </div>

          <div className="card mb-4">
            <div className="card-header">Contact Information</div>
            <div className="card-body">
              <div className="row g-3">
                <div className="col-md-6">
                  <label className="form-label">Phone Number</label>
                  <input className="form-control" value={form.phone ?? ""} onChange={set("phone")} />
                </div>
                <div className="col-md-6">
                  <label className="form-label">Email Address</label>
                  <input type="email" className="form-control" value={form.email ?? ""} onChange={set("email")} />
                </div>
                <div className="col-12">
                  <label className="form-label">Address</label>
                  <textarea
                    className="form-control"
                    rows={2}
                    value={form.address ?? ""}
                    onChange={set("address")}
                  />
                </div>
              </div>
            </div>
          </div>

          <div className="d-flex gap-2 justify-content-end">
            <Link to="/guests" className="btn btn-outline-secondary">Cancel</Link>
            <button type="submit" className="btn btn-primary" disabled={busy}>
              {busy ? "Saving…" : "Save Guest"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
