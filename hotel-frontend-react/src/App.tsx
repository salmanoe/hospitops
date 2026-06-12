import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import Layout from "./components/Layout";
import ProtectedRoute from "./components/ProtectedRoute";
import Dashboard from "./pages/Dashboard";
import GroupLogin from "./pages/GroupLogin";
import Login from "./pages/Login";
import Placeholder from "./pages/Placeholder";

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Public */}
        <Route path="/login" element={<Login />} />
        <Route path="/group/login" element={<GroupLogin />} />

        {/* Hotel-scoped app shell */}
        <Route element={<ProtectedRoute mode="hotel" />}>
          <Route element={<Layout />}>
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/reservations" element={<Placeholder title="Reservations" />} />
            <Route path="/guests" element={<Placeholder title="Guests" />} />
            <Route path="/rooms" element={<Placeholder title="Rooms" />} />
            <Route path="/housekeeping" element={<Placeholder title="Housekeeping" />} />
            <Route path="/billing" element={<Placeholder title="Billing" />} />
            <Route path="/staff" element={<Placeholder title="Staff" />} />
          </Route>
        </Route>

        {/* Group-scoped (GROUP_ADMIN) */}
        <Route element={<ProtectedRoute mode="group" />}>
          <Route path="/group/dashboard" element={<Placeholder title="Group Dashboard" />} />
        </Route>

        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
