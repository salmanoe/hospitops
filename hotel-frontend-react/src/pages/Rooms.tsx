import { useState } from "react";
import { Link } from "react-router-dom";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { api } from "../lib/api";
import { formatRp } from "../lib/utils";
import PageHeader from "../components/PageHeader";
import Pagination from "../components/Pagination";
import StatusBadge from "../components/StatusBadge";
import type { RoomStatus } from "../lib/types";

const FILTERS: { label: string; status: RoomStatus | "" }[] = [
  { label: "All", status: "" },
  { label: "Available", status: "AVAILABLE" },
  { label: "Occupied", status: "OCCUPIED" },
  { label: "Dirty", status: "DIRTY" },
  { label: "Maintenance", status: "MAINTENANCE" },
];

export default function Rooms() {
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState<RoomStatus | "">("");

  const { data, isLoading } = useQuery({
    queryKey: ["rooms-list", page, status],
    queryFn: () => api.rooms.list({ page, size: 50, ...(status ? { status } : {}) }),
    placeholderData: keepPreviousData,
  });

  const rows = data?.content ?? [];
  const count = (s: RoomStatus) => rows.filter((r) => r.status === s).length;
  const total = status ? rows.length : data?.totalElements ?? rows.length;

  const stats: [string, number | string, string][] = [
    ["Total", total, "secondary"],
    ["Available", count("AVAILABLE"), "success"],
    ["Occupied", count("OCCUPIED"), "warning"],
    ["Dirty", count("DIRTY"), "secondary"],
    ["Maintenance", count("MAINTENANCE"), "danger"],
  ];

  return (
    <div>
      <PageHeader
        title="Rooms"
        action={
          <div className="d-flex gap-2">
            <Link to="/room-types" className="btn btn-outline-secondary btn-sm">Manage Room Types →</Link>
            <Link to="/rooms/new" className="btn btn-primary btn-sm">＋ Add Room</Link>
          </div>
        }
      />
      <div className="p-4">
        <div className="row g-3 mb-4">
          {stats.map(([label, val, color]) => (
            <div key={label} className="col-6 col-md-2">
              <div className="card p-3">
                <div className="text-muted small text-uppercase">{label}</div>
                <div className={`fs-4 fw-bold text-${color}`}>{val}</div>
              </div>
            </div>
          ))}
        </div>

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
                  <th>Room</th><th>Floor</th><th>Type</th>
                  <th>Capacity</th><th>Rate/Night</th><th>Status</th><th>Notes</th><th></th>
                </tr>
              </thead>
              <tbody>
                {rows.map((r) => (
                  <tr key={r.id}>
                    <td><strong>{r.roomNumber}</strong></td>
                    <td>{r.floor ?? "—"}</td>
                    <td>{r.roomTypeName ?? "—"}</td>
                    <td>{r.capacity ?? "—"}</td>
                    <td>{formatRp(r.basePrice)}</td>
                    <td><StatusBadge status={r.status} /></td>
                    <td className="text-muted small">{r.notes || "—"}</td>
                    <td>
                      <Link to={`/rooms/${r.id}/edit`} className="btn btn-outline-secondary btn-sm">Edit</Link>
                    </td>
                  </tr>
                ))}
                {!isLoading && rows.length === 0 && (
                  <tr><td colSpan={8} className="text-center text-muted py-4">No rooms found</td></tr>
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
    </div>
  );
}
