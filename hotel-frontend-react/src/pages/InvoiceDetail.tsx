import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api, ApiError } from "../lib/api";
import { useToast } from "../lib/toast";
import { downloadBlob, formatDate, formatDateTime, formatRp, statusLabel } from "../lib/utils";
import PageHeader from "../components/PageHeader";
import StatusBadge from "../components/StatusBadge";
import type { PaymentMethod } from "../lib/types";

export default function InvoiceDetail() {
  const { id = "" } = useParams();
  const toast = useToast();
  const qc = useQueryClient();

  const [showPay, setShowPay] = useState(false);
  const [amount, setAmount] = useState(0);
  const [method, setMethod] = useState<PaymentMethod>("CASH");
  const [reference, setReference] = useState("");

  const { data: invoice, isLoading } = useQuery({
    queryKey: ["invoice", id],
    queryFn: () => api.invoices.get(id),
    enabled: !!id,
  });

  const recordPayment = useMutation({
    mutationFn: () =>
      api.invoices.recordPayment(id, { amount, method, referenceNo: reference || null }),
    onSuccess: () => {
      toast("Payment recorded");
      setShowPay(false);
      void qc.invalidateQueries({ queryKey: ["invoice", id] });
      void qc.invalidateQueries({ queryKey: ["invoices"] });
    },
    onError: (e) => toast(e instanceof ApiError ? e.message : "Payment failed", "danger"),
  });

  const openPayment = () => {
    setAmount(invoice?.balance ?? 0);
    setMethod("CASH");
    setReference("");
    setShowPay(true);
  };

  const downloadPdf = async () => {
    if (!invoice) return;
    try {
      const blob = await api.invoices.pdf(invoice.id);
      downloadBlob(blob, `${invoice.invoiceNumber}.pdf`);
    } catch {
      toast("PDF download failed", "danger");
    }
  };

  useEffect(() => {
    if (invoice) document.title = `${invoice.invoiceNumber} — HospitOps`;
  }, [invoice]);

  if (isLoading) {
    return (
      <div>
        <PageHeader title="Invoice" />
        <div className="p-4 text-muted">Loading…</div>
      </div>
    );
  }

  if (!invoice) {
    return (
      <div>
        <PageHeader title="Invoice" />
        <div className="p-4 text-muted">Invoice not found.</div>
      </div>
    );
  }

  const items = invoice.items ?? [];
  const payments = invoice.payments ?? [];

  return (
    <div>
      <PageHeader
        title={invoice.invoiceNumber || "Invoice"}
        action={
          <div className="d-flex gap-2">
            <Link to="/billing" className="btn btn-outline-secondary btn-sm">← Back</Link>
            <button className="btn btn-outline-secondary btn-sm" onClick={downloadPdf}>PDF</button>
            {invoice.paymentStatus !== "PAID" && (
              <button className="btn btn-primary btn-sm" onClick={openPayment}>Record Payment</button>
            )}
          </div>
        }
      />

      <div className="p-4">
        <div style={{ maxWidth: 760 }}>
          {/* Header card */}
          <div className="card mb-3">
            <div className="card-body">
              <div className="row g-3">
                <div className="col-md-6">
                  <div className="text-muted text-uppercase small">Invoice No.</div>
                  <div className="fw-bold">{invoice.invoiceNumber}</div>
                  <div className="mt-1"><StatusBadge status={invoice.paymentStatus} /></div>
                  <div className="text-muted text-uppercase small mt-2">Guest</div>
                  <div>{invoice.guestName || "—"}</div>
                  {invoice.reservationNumber && (
                    <>
                      <div className="text-muted text-uppercase small mt-2">Reservation</div>
                      <div>
                        {invoice.reservationId ? (
                          <Link to={`/reservations/${invoice.reservationId}`}>{invoice.reservationNumber}</Link>
                        ) : (
                          invoice.reservationNumber
                        )}
                      </div>
                    </>
                  )}
                </div>
                <div className="col-md-6 text-md-end">
                  <div className="text-muted small">Issued</div>
                  <div>{formatDateTime(invoice.issuedAt)}</div>
                  <div className="text-muted small mt-1">Due</div>
                  <div>{invoice.dueDate ? formatDate(invoice.dueDate) : "Upon checkout"}</div>
                </div>
              </div>
            </div>
          </div>

          {/* Line items */}
          <div className="card mb-3">
            <div className="card-header">Line Items</div>
            <div className="table-responsive">
              <table className="table mb-0">
                <thead>
                  <tr>
                    <th>Description</th>
                    <th className="text-center">Qty</th>
                    <th className="text-end">Unit Price</th>
                    <th className="text-end">Total</th>
                  </tr>
                </thead>
                <tbody>
                  {items.map((i, idx) => (
                    <tr key={idx}>
                      <td>{i.description}</td>
                      <td className="text-center">{i.quantity}</td>
                      <td className="text-end">{formatRp(i.unitPrice)}</td>
                      <td className="text-end">{formatRp(i.totalPrice)}</td>
                    </tr>
                  ))}
                  {items.length === 0 && (
                    <tr><td colSpan={4} className="text-center text-muted py-3">No line items</td></tr>
                  )}
                </tbody>
                <tfoot>
                  <tr className="text-muted">
                    <td colSpan={3} className="text-end">Subtotal</td>
                    <td className="text-end">{formatRp(invoice.subtotal)}</td>
                  </tr>
                  <tr className="text-muted">
                    <td colSpan={3} className="text-end">Tax</td>
                    <td className="text-end">{formatRp(invoice.taxAmount)}</td>
                  </tr>
                  {(invoice.discountAmount ?? 0) > 0 && (
                    <tr>
                      <td colSpan={3} className="text-end text-muted">Discount</td>
                      <td className="text-end text-success">− {formatRp(invoice.discountAmount)}</td>
                    </tr>
                  )}
                  <tr>
                    <td colSpan={3} className="text-end fw-bold">Total</td>
                    <td className="text-end fw-bold">{formatRp(invoice.totalAmount)}</td>
                  </tr>
                  <tr className="text-success">
                    <td colSpan={3} className="text-end">Paid</td>
                    <td className="text-end">{formatRp(invoice.totalPaid)}</td>
                  </tr>
                  <tr className={invoice.balance > 0 ? "text-danger fw-bold" : "text-muted"}>
                    <td colSpan={3} className="text-end">Balance</td>
                    <td className="text-end">{formatRp(invoice.balance)}</td>
                  </tr>
                </tfoot>
              </table>
            </div>
          </div>

          {/* Payment history */}
          {payments.length > 0 && (
            <div className="card mb-3">
              <div className="card-header">Payment History</div>
              <div className="table-responsive">
                <table className="table mb-0">
                  <thead>
                    <tr>
                      <th>Date</th>
                      <th>Method</th>
                      <th>Reference</th>
                      <th className="text-end">Amount</th>
                    </tr>
                  </thead>
                  <tbody>
                    {payments.map((p, idx) => (
                      <tr key={idx}>
                        <td>{formatDateTime(p.paidAt)}</td>
                        <td>{statusLabel(p.method)}</td>
                        <td className="text-muted small">{p.referenceNo || "—"}</td>
                        <td className="text-end text-success">{formatRp(p.amount)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Record-payment modal */}
      {showPay && (
        <>
          <div className="modal d-block" tabIndex={-1} role="dialog">
            <div className="modal-dialog">
              <div className="modal-content">
                <div className="modal-header">
                  <h5 className="modal-title">Record Payment</h5>
                  <button type="button" className="btn-close" onClick={() => setShowPay(false)} />
                </div>
                <div className="modal-body">
                  <div className="mb-3">
                    <label className="form-label">Balance Due</label>
                    <div className="fw-bold fs-5">{formatRp(invoice.balance)}</div>
                  </div>
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
                  <button className="btn btn-outline-secondary" onClick={() => setShowPay(false)}>Cancel</button>
                  <button
                    className="btn btn-success"
                    disabled={recordPayment.isPending}
                    onClick={() => recordPayment.mutate()}
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