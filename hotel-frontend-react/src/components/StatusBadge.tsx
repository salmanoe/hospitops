import { statusColor, statusLabel } from "../lib/utils";

export default function StatusBadge({ status }: { status: string }) {
  return <span className={`badge bg-${statusColor(status)}`}>{statusLabel(status)}</span>;
}
