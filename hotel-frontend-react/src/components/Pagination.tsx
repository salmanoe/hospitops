export default function Pagination({
  page,
  totalPages,
  onChange,
}: {
  page: number;
  totalPages: number;
  onChange: (p: number) => void;
}) {
  if (totalPages <= 1) return null;
  return (
    <ul className="pagination pagination-sm mb-0">
      {Array.from({ length: totalPages }, (_, i) => (
        <li key={i} className={"page-item" + (i === page ? " active" : "")}>
          <button className="page-link" onClick={() => onChange(i)}>
            {i + 1}
          </button>
        </li>
      ))}
    </ul>
  );
}
