import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import Layout from "./components/Layout";
import ProtectedRoute from "./components/ProtectedRoute";
import GroupLayout from "./components/GroupLayout";
import Billing from "./pages/Billing";
import CalendarBookings from "./pages/CalendarBookings";
import CalendarRates from "./pages/CalendarRates";
import Channels from "./pages/Channels";
import Dashboard from "./pages/Dashboard";
import GroupDashboard from "./pages/GroupDashboard";
import GroupLogin from "./pages/GroupLogin";
import GroupProfile from "./pages/GroupProfile";
import GroupSignup from "./pages/GroupSignup";
import GuestForm from "./pages/GuestForm";
import Guests from "./pages/Guests";
import HotelForm from "./pages/HotelForm";
import HotelPolicy from "./pages/HotelPolicy";
import HotelSetup from "./pages/HotelSetup";
import HotelsList from "./pages/HotelsList";
import Housekeeping from "./pages/Housekeeping";
import Login from "./pages/Login";
import ReservationDetail from "./pages/ReservationDetail";
import ReservationNew from "./pages/ReservationNew";
import Reservations from "./pages/Reservations";
import RoomForm from "./pages/RoomForm";
import Rooms from "./pages/Rooms";
import RoomTypes from "./pages/RoomTypes";
import Staff from "./pages/Staff";
import StaffForm from "./pages/StaffForm";

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Public */}
        <Route path="/login" element={<Login />} />
        <Route path="/group/login" element={<GroupLogin />} />
        <Route path="/group/signup" element={<GroupSignup />} />

        {/* Hotel-scoped app shell */}
        <Route element={<ProtectedRoute mode="hotel" />}>
          <Route element={<Layout />}>
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/reservations" element={<Reservations />} />
            <Route path="/calendar" element={<CalendarBookings />} />
            <Route path="/rates" element={<CalendarRates />} />
            <Route path="/reservations/new" element={<ReservationNew />} />
            <Route path="/reservations/:id" element={<ReservationDetail />} />
            <Route path="/guests" element={<Guests />} />
            <Route path="/guests/new" element={<GuestForm />} />
            <Route path="/guests/:id/edit" element={<GuestForm />} />
            <Route path="/rooms" element={<Rooms />} />
            <Route path="/rooms/new" element={<RoomForm />} />
            <Route path="/rooms/:id/edit" element={<RoomForm />} />
            <Route path="/room-types" element={<RoomTypes />} />
            <Route path="/housekeeping" element={<Housekeeping />} />
            <Route path="/billing" element={<Billing />} />
            <Route path="/channels" element={<Channels />} />
            <Route path="/staff" element={<Staff />} />
            <Route path="/staff/new" element={<StaffForm />} />
            <Route path="/staff/:id/edit" element={<StaffForm />} />
          </Route>
        </Route>

        {/* Group-scoped (GROUP_ADMIN) */}
        <Route element={<ProtectedRoute mode="group" />}>
          <Route element={<GroupLayout />}>
            <Route path="/group/dashboard" element={<GroupDashboard />} />
            <Route path="/group/hotels" element={<HotelsList />} />
            <Route path="/group/hotels/new" element={<HotelForm />} />
            <Route path="/group/hotels/:id/setup" element={<HotelSetup />} />
            <Route path="/group/hotels/:id/policy" element={<HotelPolicy />} />
            <Route path="/group/profile" element={<GroupProfile />} />
          </Route>
        </Route>

        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
