import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../lib/auth";
import type { Role } from "../lib/types";

interface NavItem {
  to: string;
  label: string;
  roles: Role[];
}

// Hotel-staff navigation, role-gated (mirrors the legacy sidebar [data-roles]).
const NAV: NavItem[] = [
  { to: "/dashboard", label: "Dashboard", roles: ["GROUP_ADMIN", "ADMIN", "MANAGER", "FRONT_DESK", "ACCOUNTANT", "HOUSEKEEPING"] },
  { to: "/calendar", label: "Calendar", roles: ["GROUP_ADMIN", "ADMIN", "MANAGER", "FRONT_DESK"] },
  { to: "/reservations", label: "Reservations", roles: ["GROUP_ADMIN", "ADMIN", "MANAGER", "FRONT_DESK"] },
  { to: "/rates", label: "Rates", roles: ["GROUP_ADMIN", "ADMIN", "MANAGER"] },
  { to: "/guests", label: "Guests", roles: ["GROUP_ADMIN", "ADMIN", "MANAGER", "FRONT_DESK"] },
  { to: "/rooms", label: "Rooms", roles: ["GROUP_ADMIN", "ADMIN", "MANAGER"] },
  { to: "/housekeeping", label: "Housekeeping", roles: ["GROUP_ADMIN", "ADMIN", "MANAGER", "HOUSEKEEPING"] },
  { to: "/billing", label: "Billing", roles: ["GROUP_ADMIN", "ADMIN", "MANAGER", "ACCOUNTANT"] },
  { to: "/channels", label: "Channels", roles: ["GROUP_ADMIN", "ADMIN", "MANAGER"] },
  { to: "/staff", label: "Staff", roles: ["GROUP_ADMIN", "ADMIN"] },
];

export default function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const visible = NAV.filter((n) => user && n.roles.includes(user.role));

  const onLogout = async () => {
    await logout();
    navigate("/login", { replace: true });
  };

  return (
    <div className="d-flex" style={{ minHeight: "100vh" }}>
      <aside className="bg-dark text-white p-3 d-flex flex-column" style={{ width: 240 }}>
        <h5 className="mb-4 fw-bold">HospitOps</h5>
        <nav className="nav nav-pills flex-column gap-1 flex-grow-1">
          {visible.map((n) => (
            <NavLink
              key={n.to}
              to={n.to}
              className={({ isActive }) =>
                "nav-link text-white" + (isActive ? " active" : "")
              }
            >
              {n.label}
            </NavLink>
          ))}
        </nav>
        <div className="border-top border-secondary pt-3 mt-3">
          <div className="small text-secondary">{user?.name}</div>
          <div className="small text-secondary mb-2">{user?.role.replace(/_/g, " ")}</div>
          <button className="btn btn-outline-light btn-sm w-100" onClick={onLogout}>
            Log out
          </button>
        </div>
      </aside>

      <main className="flex-grow-1 bg-light">
        <Outlet />
      </main>
    </div>
  );
}
