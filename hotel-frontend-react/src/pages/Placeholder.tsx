export default function Placeholder({ title }: { title: string }) {
  return (
    <div>
      <div className="d-flex justify-content-between align-items-center px-4 py-3 bg-white border-bottom">
        <h2 className="h4 mb-0">{title}</h2>
      </div>
      <div className="p-4">
        <div className="card">
          <div className="card-body text-center text-muted py-5">
            <p className="mb-1 fw-semibold">{title} — coming soon</p>
            <p className="small mb-0">
              This screen is being ported from the legacy frontend in a later step.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
