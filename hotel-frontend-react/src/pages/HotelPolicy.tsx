import { useEffect, useState, type FormEvent } from "react";
import { Link, useNavigate, useParams, useSearchParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { api, ApiError } from "../lib/api";
import { useToast } from "../lib/toast";
import type { PolicyConfig } from "../lib/types";

const EMPTY: PolicyConfig = {
  taxPercent: 11,
  taxName: "",
  invoiceHotelName: "",
  invoiceAddress: "",
  invoiceFooterNote: "",
};

export default function HotelPolicy() {
  const { id = "" } = useParams();
  const [params] = useSearchParams();
  const fromSetup = params.get("setup") === "1";
  const navigate = useNavigate();
  const toast = useToast();

  const [form, setForm] = useState<PolicyConfig>(EMPTY);
  const [busy, setBusy] = useState(false);

  const hotel = useQuery({ queryKey: ["hotel", id], queryFn: () => api.hotels.get(id) });
  // Policy may not exist yet (404) — treat absence as the empty form.
  const policy = useQuery({
    queryKey: ["hotel-policy", id],
    queryFn: () => api.hotelPolicy.get(id),
    retry: false,
  });

  useEffect(() => {
    if (policy.data) {
      setForm({
        taxPercent: policy.data.taxPercent ?? 0,
        taxName: policy.data.taxName ?? "",
        invoiceHotelName: policy.data.invoiceHotelName ?? "",
        invoiceAddress: policy.data.invoiceAddress ?? "",
        invoiceFooterNote: policy.data.invoiceFooterNote ?? "",
      });
    }
  }, [policy.data]);

  const set = <K extends keyof PolicyConfig>(k: K, v: PolicyConfig[K]) =>
    setForm((f) => ({ ...f, [k]: v }));

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setBusy(true);
    try {
      await api.hotelPolicy.save(id, {
        taxPercent: Number(form.taxPercent),
        taxName: form.taxName.trim(),
        invoiceHotelName: form.invoiceHotelName.trim(),
        invoiceAddress: form.invoiceAddress?.toString().trim() || null,
        invoiceFooterNote: form.invoiceFooterNote?.toString().trim() || null,
      });
      // Saving the policy auto-completes the POLICY setup step on the backend.
      if (fromSetup) {
        toast("Policy saved — continuing setup…");
        navigate(`/group/hotels/${id}/setup`);
        return;
      }
      toast("Policy saved successfully.");
      setBusy(false);
    } catch (err) {
      toast(err instanceof ApiError ? err.message : "Failed to save policy", "danger");
      setBusy(false);
    }
  };

  return (
    <div>
      <h2 className="h4 mb-1">{hotel.data?.name ? `${hotel.data.name} — Policy` : "Policy Config"}</h2>
      <p className="text-muted small mb-4">Tax settings and invoice branding</p>

      {fromSetup && (
        <div className="alert alert-info d-flex justify-content-between align-items-center">
          <div>
            <strong>Setup Step 2 of 5 — Policy Config</strong>
            <div className="small">Save the policy to auto-complete this step and return to the wizard.</div>
          </div>
          <Link to={`/group/hotels/${id}/setup`} className="btn btn-outline-primary btn-sm">← Back to Setup</Link>
        </div>
      )}

      <div className="card" style={{ maxWidth: 600 }}>
        <div className="card-body">
          <form onSubmit={onSubmit}>
            <div className="text-muted text-uppercase mb-2" style={{ fontSize: 10, letterSpacing: 1.5 }}>Tax</div>
            <div className="row g-3 mb-3">
              <div className="col-sm-4">
                <label className="form-label fw-semibold">Tax Rate (%)</label>
                <input type="number" className="form-control" min={0} max={100} step={1}
                  value={form.taxPercent} onChange={(e) => set("taxPercent", Number(e.target.value))} required />
              </div>
              <div className="col-sm-8">
                <label className="form-label fw-semibold">Tax Label</label>
                <input className="form-control" maxLength={50} placeholder="PPN"
                  value={form.taxName} onChange={(e) => set("taxName", e.target.value)} required />
                <div className="form-text">Shown on invoices, e.g. PPN, VAT, GST.</div>
              </div>
            </div>

            <div className="text-muted text-uppercase mb-2" style={{ fontSize: 10, letterSpacing: 1.5 }}>Invoice Branding</div>
            <div className="mb-3">
              <label className="form-label fw-semibold">Hotel Name on Invoice</label>
              <input className="form-control" maxLength={200} placeholder="The Grand Nusantara"
                value={form.invoiceHotelName} onChange={(e) => set("invoiceHotelName", e.target.value)} required />
            </div>
            <div className="mb-3">
              <label className="form-label fw-semibold">Invoice Address <span className="text-muted fw-normal">(optional)</span></label>
              <textarea className="form-control" rows={2} placeholder="Jl. Sudirman No. 1, Jakarta"
                value={form.invoiceAddress ?? ""} onChange={(e) => set("invoiceAddress", e.target.value)} />
            </div>
            <div className="mb-4">
              <label className="form-label fw-semibold">Invoice Footer Note <span className="text-muted fw-normal">(optional)</span></label>
              <textarea className="form-control" rows={2} placeholder="Thank you for staying with us."
                value={form.invoiceFooterNote ?? ""} onChange={(e) => set("invoiceFooterNote", e.target.value)} />
            </div>

            <div className="d-flex gap-2">
              <button type="submit" className="btn btn-primary" disabled={busy}>
                {busy ? "Saving…" : "Save Policy"}
              </button>
              <Link to={fromSetup ? `/group/hotels/${id}/setup` : "/group/hotels"} className="btn btn-outline-secondary">
                Cancel
              </Link>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
