// Formatters mirroring core.js Utils — Indonesian locale (Rp, id-ID dates).

export const formatRp = (amount: number | null | undefined): string => {
  if (amount == null) return "—";
  return "Rp " + Number(amount).toLocaleString("id-ID", {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  });
};

export const formatDate = (dateStr?: string | null): string => {
  if (!dateStr) return "—";
  return new Date(dateStr).toLocaleDateString("id-ID", {
    day: "2-digit",
    month: "short",
    year: "numeric",
  });
};

export const formatDateTime = (dateStr?: string | null): string => {
  if (!dateStr) return "—";
  return new Date(dateStr).toLocaleString("id-ID", {
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
};

export const formatNights = (n: number): string => `${n} night${n !== 1 ? "s" : ""}`;

/** Bootstrap contextual colour for a domain status (rooms, reservations, invoices). */
export const statusColor = (status: string): string => {
  const map: Record<string, string> = {
    AVAILABLE: "success",
    OCCUPIED: "warning",
    DIRTY: "secondary",
    MAINTENANCE: "danger",
    SERVICE_REQUESTED: "info",
    CONFIRMED: "info",
    CHECKED_IN: "warning",
    CHECKED_OUT: "success",
    CANCELLED: "danger",
    PENDING: "secondary",
    UNPAID: "danger",
    PARTIAL: "warning",
    PAID: "success",
  };
  return map[status] ?? "secondary";
};

export const statusLabel = (status: string): string => status.replace(/_/g, " ");

/** Trigger a browser download for a Blob (e.g. an invoice PDF). */
export const downloadBlob = (blob: Blob, filename: string): void => {
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
};
