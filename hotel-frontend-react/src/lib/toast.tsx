// Minimal toast system (replaces core.js Utils.toast / Bootstrap Toast JS) using
// React state — no imperative Bootstrap JS needed.
import { createContext, useCallback, useContext, useState, type ReactNode } from "react";

type ToastType = "success" | "danger" | "info" | "warning";
interface ToastItem {
  id: number;
  message: string;
  type: ToastType;
}

const ToastContext = createContext<(message: string, type?: ToastType) => void>(() => {});

export const useToast = () => useContext(ToastContext);

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([]);

  const push = useCallback((message: string, type: ToastType = "success") => {
    const id = Date.now() + Math.random();
    setToasts((t) => [...t, { id, message, type }]);
    setTimeout(() => setToasts((t) => t.filter((x) => x.id !== id)), 3500);
  }, []);

  return (
    <ToastContext.Provider value={push}>
      {children}
      <div
        className="toast-container position-fixed bottom-0 end-0 p-3"
        style={{ zIndex: 9999 }}
      >
        {toasts.map((t) => (
          <div key={t.id} className={`toast show align-items-center text-bg-${t.type} border-0`} role="alert">
            <div className="d-flex">
              <div className="toast-body fw-semibold">{t.message}</div>
            </div>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}
