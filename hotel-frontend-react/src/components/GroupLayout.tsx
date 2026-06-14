import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../lib/auth";

export default function GroupLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const onLogout = async () => {
    await logout();
    navigate("/group/login", { replace: true });
  };

  const link = ({ isActive }: { isActive: boolean }) =>
    "small text-decoration-none " + (isActive ? "fw-semibold" : "text-muted");

  return (
    <div className="bg-light" style={{ minHeight: "100vh" }}>
      <div className="d-flex justify-content-between align-items-center px-4 py-3 bg-white border-bottom sticky-top flex-wrap gap-2">
        <div className="fw-bold">
          HospitOps <span className="text-muted small">· GROUP ADMIN</span>
        </div>
        <div className="d-flex align-items-center gap-3 flex-wrap">
          <NavLink to="/group/dashboard" className={link}>Dashboard</NavLink>
          <NavLink to="/group/hotels" className={link}>Hotels</NavLink>
          <NavLink to="/group/profile" className={link}>Profile</NavLink>
          <span className="text-muted small d-none d-sm-inline">{user?.username}</span>
          <button className="btn btn-link text-danger p-0 small text-decoration-none" onClick={onLogout}>
            Sign out
          </button>
        </div>
      </div>
      <div className="p-4">
        <Outlet />
      </div>
    </div>
  );
}
