import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "../lib/auth";

type Mode = "hotel" | "group";

/**
 * Route guard mirroring core.js requireHotelSession / requireGroupAdmin:
 *   - "hotel": any logged-in hotel staff, or a GROUP_ADMIN who has entered a
 *      hotel (hotel-scoped token). A group-scoped GROUP_ADMIN is bounced to the
 *      group dashboard to use "Enter Hotel" first.
 *   - "group": GROUP_ADMIN sessions only.
 */
export default function ProtectedRoute({ mode }: { mode: Mode }) {
  const { isLoggedIn, isGroupAdmin, isHotelScoped } = useAuth();

  if (mode === "group") {
    if (!isLoggedIn) return <Navigate to="/group/login" replace />;
    if (!isGroupAdmin) return <Navigate to="/group/login" replace />;
    return <Outlet />;
  }

  // mode === "hotel"
  if (!isLoggedIn) return <Navigate to="/login" replace />;
  if (isGroupAdmin && !isHotelScoped) return <Navigate to="/group/dashboard" replace />;
  return <Outlet />;
}
