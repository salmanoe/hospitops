import { useState } from "react";
import { Link } from "react-router-dom";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { api } from "../lib/api";
import { useDebounce } from "../lib/useDebounce";
import { formatDate } from "../lib/utils";
import PageHeader from "../components/PageHeader";
import Pagination from "../components/Pagination";

export default function Guests() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const q = useDebounce(search, 350);

  const { data, isLoading } = useQuery({
    queryKey: ["guests", page, q],
    queryFn: () => api.guests.list({ page, size: 20, ...(q ? { q } : {}) }),
    placeholderData: keepPreviousData,
  });

  const rows = data?.content ?? [];

  return (
    <div>
      <PageHeader
        title="Guests"
        action={<Link to="/guests/new" className="btn btn-primary btn-sm">＋ Register Guest</Link>}
      />
      <div className="p-4">
        <div className="mb-3">
          <input
            className="form-control"
            style={{ maxWidth: 340 }}
            placeholder="Search by name or ID number…"
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              setPage(0);
            }}
          />
        </div>

        <div className="card">
          <div className="table-responsive">
            <table className="table table-hover mb-0">
              <thead>
                <tr>
                  <th>Name</th><th>ID Number</th><th>Nationality</th>
                  <th>Phone</th><th>Email</th><th>Registered</th><th></th>
                </tr>
              </thead>
              <tbody>
                {rows.map((g) => (
                  <tr key={g.id}>
                    <td><strong>{g.fullName}</strong></td>
                    <td className="text-muted">{g.idNumber || "—"}</td>
                    <td>{g.nationality || "—"}</td>
                    <td>{g.phone || "—"}</td>
                    <td>{g.email || "—"}</td>
                    <td>{formatDate(g.createdAt)}</td>
                    <td>
                      <Link to={`/guests/${g.id}/edit`} className="btn btn-outline-secondary btn-sm">
                        Edit
                      </Link>
                    </td>
                  </tr>
                ))}
                {!isLoading && rows.length === 0 && (
                  <tr><td colSpan={7} className="text-center text-muted py-4">No guests found</td></tr>
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
