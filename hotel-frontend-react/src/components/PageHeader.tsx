import type { ReactNode } from "react";

export default function PageHeader({ title, action }: { title: string; action?: ReactNode }) {
  return (
    <div className="d-flex justify-content-between align-items-center px-4 py-3 bg-white border-bottom">
      <h2 className="h4 mb-0">{title}</h2>
      {action}
    </div>
  );
}
