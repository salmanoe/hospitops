import { useState } from "react";
import { Link } from "react-router-dom";
import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api, ApiError } from "../lib/api";
import { useToast } from "../lib/toast";
import { downloadBlob, formatRp } from "../lib/utils";
import PageHeader from "../components/PageHeader";
import Pagination from "../components/Pagination";
import StatusBadge from "../components/StatusBadge";
import { formatDate } from "../lib/utils";
import type { Invoice, PaymentMethod, PaymentStatus } from "../lib/types";

const FILTERS: { label: string; status: PaymentStatus | "" }[] = [
  { label: "All", status: "" },
  { label: "Unpaid", status: "UNPAID" },
  { label: "Partial", status: "PARTIAL" },
  { label: "Paid", status: "PAID" },
];

interface PayTarget {
  id: string;
  balance: number;
}

export default function Billing() {
  const toast = useToast();
  const qc = useQueryClient();
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState<PaymentStatus | "">("");
  const [pay, setPay] = useState<PayTarget | null>(null);
  const [amount, setAmount] = useState(0);
  const [method, setMethod] = useState<PaymentMethod>("CASH");
  const [reference, setReference] = useState("");

  const { data, isLoading } = useQuery({
    queryKey: ["invoices", page, status],
    queryFn: () => api.invoices.list({ page, size: 20, ...(status ? { status } : {}) }),
    placeholderData: keepPreviousData,
  });

  const recordPayment = useMutation({
    mutationFn: (target: PayTarget) =>
      api.invoices.recordPayment(target.id, { amount, method, referenceNo: reference || null }),
    onSuccess: () => {
      toast("Payment recorded");
      setPay(null);
      void qc.invalidateQueries({ queryKey: ["invoices"] });
    },
    onError: (e) => toast(e instanceof ApiError ? e.message : "Payment failed", "danger"),
  });

  const openPayment = (inv: Invoice) => {
    setPay({ id: inv.id, balance: inv.balance });
    setAmount(inv.balance);
    setMethod("CASH");
    setReference("");
  };

  const downloadPdf = async (inv: Invoice) => {
    try {
      const blob = await api.invoices.pdf(inv.id);
      downloadBlob(blob, `${inv.invoiceNumber}.pdf`);
    } catch {
      toast("PDF download failed", "danger");
    }
  };

  const rows = data?.content ?? [];

  return (
    <div>
      <PageHeader title="Invoices" />
      <div className="p-4">
        <div className="d-flex gap-2 flex-wrap mb-3">
          {FILTERS.map((f) => (
            <button
              key={f.label}
              className={"btn btn-sm " + (status === f.status ? "btn-primary" : "btn-outline-secondary")}
              onClick={() => { setStatus(f.status); setPage(0); }}
            >
              {f.label}
            </button>
          ))}
        </div>

        <div className="card">
          <div className="table-responsive">
            <table className="table table-hover mb-0">
              <thead>
                <tr>
                  <th>Invoice</th><th>Reservation</th><th>Total</th>
                  <th>Paid</th><th>Balance</th><th>Status</th><th>Issued</th><th></th>
                </tr>
              </thead>
              <tbody>
                {rows.map((inv) => (
                  <tr key={inv.id}>
                    <td><Link to={`/billing/${inv.id}`} className="small">{inv.invoiceNumber}</Link></td>
                    <td>
                      <small className="text-muted">{inv.reservationNumber}</small>
                      {inv.guestName && <><br /><span className="fw-semibold small">{inv.guestName}</span></>}
                    </td>
                    <td>{formatRp(inv.totalAmount)}</td>
                    <td className="text-success">{formatRp(inv.totalPaid)}</td>
                    <td className={inv.balance > 0 ? "text-danger" : ""}>{formatRp(inv.balance)}</td>
                    <td><StatusBadge status={inv.paymentStatus} /></td>
                    <td>{formatDate(inv.issuedAt)}</td>
                    <td>
                      <div className="d-flex gap-1">
                        {inv.paymentStatus !== "PAID" && (
                          <button className="btn btn-primary btn-sm" onClick={() => openPayment(inv)}>Pay</button>
                        )}
                        <button className="btn btn-outline-secondary btn-sm" onClick={() => downloadPdf(inv)}>PDF</button>
                      </div>
                    </td>
                  </tr>
                ))}
                {!isLoading && rows.length === 0 && (
                  <tr><td colSpan={8} className="text-center text-muted py-4">No invoices found</td></tr>
                )}
              </tbody>
            </table>
          </div>
          {data && (
            <div className="card-body pt-2">
              <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />
            </div>
          )}
        </div>
      </div>

      {/* Record-payment modal */}
      {pay && (
        <>
          <div className="modal d-block" tabIndex={-1} role="dialog">
            <div className="modal-dialog">
              <div className="modal-content">
                <div className="modal-header">
                  <h5 className="modal-title">Record Payment</h5>
                  <button type="button" className="btn-close" onClick={() => setPay(null)} />
                </div>
                <div className="modal-body">
                  <div className="mb-3">
                    <label className="form-label">Amount (IDR) *</label>
                    <input
                      type="number"
                      className="form-control"
                      min={1000}
                      step={1000}
                      value={amount}
                      onChange={(e) => setAmount(Number(e.target.value))}
                    />
                  </div>
                  <div className="mb-3">
                    <label className="form-label">Method *</label>
                    <select className="form-select" value={method} onChange={(e) => setMethod(e.target.value as PaymentMethod)}>
                      <option value="CASH">Cash</option>
                      <option value="CREDIT_CARD">Credit Card</option>
                      <option value="DEBIT_CARD">Debit Card</option>
                      <option value="BANK_TRANSFER">Bank Transfer</option>
                    </select>
                  </div>
                  <div className="mb-3">
                    <label className="form-label">Reference No.</label>
                    <input
                      className="form-control"
                      placeholder="Card / transfer reference"
                      value={reference}
                      onChange={(e) => setReference(e.target.value)}
                    />
                  </div>
                </div>
                <div className="modal-footer">
                  <button className="btn btn-outline-secondary" onClick={() => setPay(null)}>Cancel</button>
                  <button
                    className="btn btn-success"
                    disabled={recordPayment.isPending}
                    onClick={() => recordPayment.mutate(pay)}
                  >
                    Record Payment
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
