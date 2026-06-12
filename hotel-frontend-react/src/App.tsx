import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import Layout from "./components/Layout";
import ProtectedRoute from "./components/ProtectedRoute";
import Billing from "./pages/Billing";
import Dashboard from "./pages/Dashboard";
import GroupDashboard from "./pages/GroupDashboard";
import GroupLogin from "./pages/GroupLogin";
import GuestForm from "./pages/GuestForm";
import Guests from "./pages/Guests";
import Housekeeping from "./pages/Housekeeping";
import Login from "./pages/Login";
import Placeholder from "./pages/Placeholder";
import Reservations from "./pages/Reservations";
import Rooms from "./pages/Rooms";
import Staff from "./pages/Staff";

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
            <Route path="/reservations" element={<Reservations />} />
            <Route path="/reservations/new" element={<Placeholder title="New Booking" />} />
            <Route path="/reservations/:id" element={<Placeholder title="Reservation Detail" />} />
            <Route path="/guests" element={<Guests />} />
            <Route path="/guests/new" element={<GuestForm />} />
            <Route path="/guests/:id/edit" element={<GuestForm />} />
            <Route path="/rooms" element={<Rooms />} />
            <Route path="/housekeeping" element={<Housekeeping />} />
            <Route path="/billing" element={<Billing />} />
            <Route path="/staff" element={<Staff />} />
          </Route>
        </Route>

        {/* Group-scoped (GROUP_ADMIN) */}
        <Route element={<ProtectedRoute mode="group" />}>
          <Route path="/group/dashboard" element={<GroupDashboard />} />
          <Route path="/group/hotels" element={<Placeholder title="Hotels" />} />
          <Route path="/group/profile" element={<Placeholder title="Group Profile" />} />
        </Route>

        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
