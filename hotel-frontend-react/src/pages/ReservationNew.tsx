import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { keepPreviousData, useMutation, useQuery } from "@tanstack/react-query";
import { api, ApiError } from "../lib/api";
import { useToast } from "../lib/toast";
import { useDebounce } from "../lib/useDebounce";
import { formatRp } from "../lib/utils";
import PageHeader from "../components/PageHeader";
import type { Room } from "../lib/types";

const todayIso = () => new Date().toISOString().split("T")[0];

export default function ReservationNew() {
  const navigate = useNavigate();
  const toast = useToast();

  const [guestQuery, setGuestQuery] = useState("");
  const [guestId, setGuestId] = useState("");
  const [guestName, setGuestName] = useState("");
  const [showSuggestions, setShowSuggestions] = useState(false);

  const [checkIn, setCheckIn] = useState("");
  const [checkOut, setCheckOut] = useState("");
  const [adults, setAdults] = useState(1);
  const [children, setChildren] = useState(0);
  const [specialRequests, setSpecialRequests] = useState("");

  const [rooms, setRooms] = useState<Room[] | null>(null);
  const [roomId, setRoomId] = useState("");

  const debouncedQuery = useDebounce(guestQuery, 300);
  const suggestions = useQuery({
    queryKey: ["guest-search", debouncedQuery],
    queryFn: () => api.guests.search(debouncedQuery),
    enabled: debouncedQuery.trim().length >= 2 && showSuggestions,
    placeholderData: keepPreviousData,
  });

  const findRooms = useMutation({
    mutationFn: () => api.rooms.available(checkIn, checkOut),
    onSuccess: (data) => { setRooms(data); setRoomId(""); },
    onError: (e) => toast(e instanceof ApiError ? e.message : "Failed to find rooms", "danger"),
  });

  const create = useMutation({
    mutationFn: () =>
      api.reservations.create({
        guestId,
        roomId,
        checkIn,
        checkOut,
        adults,
        children,
        specialRequests: specialRequests || null,
      }),
    onSuccess: () => { toast("Booking confirmed!"); navigate("/reservations"); },
    onError: (e) => toast(e instanceof ApiError ? e.message : "Booking failed", "danger"),
  });

  const onFindRooms = () => {
    if (!checkIn || !checkOut) {
      toast("Please select check-in and check-out dates", "warning");
      return;
    }
    findRooms.mutate();
  };

  const onSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (!guestId) return toast("Please select a guest", "warning");
    if (!roomId) return toast("Please select a room", "warning");
    create.mutate();
  };

  const pickGuest = (id: string, name: string) => {
    setGuestId(id);
    setGuestName(name);
    setGuestQuery(name);
    setShowSuggestions(false);
  };

  return (
    <div>
      <PageHeader
        title="New Booking"
        action={<Link to="/reservations" className="btn btn-outline-secondary btn-sm">← Back</Link>}
      />
      <div className="p-4" style={{ maxWidth: 820 }}>
        <form onSubmit={onSubmit}>
          {/* Guest */}
          <div className="card mb-3">
            <div className="card-header">Guest</div>
            <div className="card-body">
              <div className="row g-3">
                <div className="col-md-6 position-relative">
                  <label className="form-label">Search Guest</label>
                  <input
                    className="form-control"
                    placeholder="Name or ID number…"
                    autoComplete="off"
                    value={guestQuery}
                    onChange={(e) => { setGuestQuery(e.target.value); setShowSuggestions(true); setGuestId(""); }}
                  />
                  {showSuggestions && debouncedQuery.trim().length >= 2 && (
                    <div className="list-group position-absolute" style={{ zIndex: 999, width: "calc(100% - 24px)" }}>
                      {(suggestions.data ?? []).map((g) => (
                        <button
                          key={g.id}
                          type="button"
                          className="list-group-item list-group-item-action"
                          onClick={() => pickGuest(g.id, g.fullName)}
                        >
                          <strong>{g.fullName}</strong>
                          <small className="text-muted ms-2">{g.idNumber || ""}</small>
                        </button>
                      ))}
                      {suggestions.data?.length === 0 && (
                        <div className="list-group-item text-muted">No results</div>
                      )}
                    </div>
                  )}
                </div>
                <div className="col-md-6">
                  <label className="form-label">Selected Guest</label>
                  <input className="form-control" readOnly placeholder="No guest selected" value={guestName} />
                </div>
              </div>
              <div className="mt-2">
                <Link to="/guests/new" target="_blank" className="btn btn-outline-secondary btn-sm">
                  ＋ Register New Guest
                </Link>
              </div>
            </div>
          </div>

          {/* Booking details */}
          <div className="card mb-3">
            <div className="card-header">Booking Details</div>
            <div className="card-body">
              <div className="row g-3">
                <div className="col-md-3">
                  <label className="form-label">Check-In *</label>
                  <input
                    type="date"
                    className="form-control"
                    min={todayIso()}
                    value={checkIn}
                    onChange={(e) => {
                      setCheckIn(e.target.value);
                      if (checkOut && checkOut <= e.target.value) setCheckOut("");
                    }}
                    required
                  />
                </div>
                <div className="col-md-3">
                  <label className="form-label">Check-Out *</label>
                  <input
                    type="date"
                    className="form-control"
                    min={checkIn || todayIso()}
                    value={checkOut}
                    onChange={(e) => setCheckOut(e.target.value)}
                    required
                  />
                </div>
                <div className="col-md-3">
                  <label className="form-label">Adults</label>
                  <input type="number" className="form-control" min={1} max={10} value={adults}
                    onChange={(e) => setAdults(Number(e.target.value))} />
                </div>
                <div className="col-md-3">
                  <label className="form-label">Children</label>
                  <input type="number" className="form-control" min={0} max={10} value={children}
                    onChange={(e) => setChildren(Number(e.target.value))} />
                </div>
              </div>
            </div>
          </div>

          {/* Room selection */}
          <div className="card mb-3">
            <div className="card-header d-flex justify-content-between align-items-center">
              <span>Select Room</span>
              <button type="button" className="btn btn-primary btn-sm" disabled={findRooms.isPending} onClick={onFindRooms}>
                Find Available Rooms
              </button>
            </div>
            <div className="card-body">
              {rooms === null ? (
                <div className="text-muted text-center py-3">Enter dates and click "Find Available Rooms"</div>
              ) : rooms.length === 0 ? (
                <div className="text-muted text-center py-3">No rooms available for selected dates</div>
              ) : (
                <div className="table-responsive">
                  <table className="table table-hover mb-0">
                    <thead>
                      <tr><th></th><th>Room</th><th>Type</th><th>Floor</th><th>Capacity</th><th>Rate/Night</th></tr>
                    </thead>
                    <tbody>
                      {rooms.map((r) => (
                        <tr
                          key={r.id}
                          className={roomId === r.id ? "table-active" : ""}
                          style={{ cursor: "pointer" }}
                          onClick={() => setRoomId(r.id)}
                        >
                          <td><input type="radio" name="roomSelect" checked={roomId === r.id} readOnly /></td>
                          <td><strong>{r.roomNumber}</strong></td>
                          <td>{r.roomTypeName}</td>
                          <td>{r.floor}</td>
                          <td>{r.capacity} pax</td>
                          <td>{formatRp(r.effectiveRate ?? r.basePrice)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </div>

          {/* Additional info */}
          <div className="card mb-4">
            <div className="card-header">Additional Info</div>
            <div className="card-body">
              <label className="form-label">Special Requests</label>
              <textarea
                className="form-control"
                rows={3}
                placeholder="e.g. high floor, extra bed, late check-in…"
                value={specialRequests}
                onChange={(e) => setSpecialRequests(e.target.value)}
              />
            </div>
          </div>

          <div className="d-flex gap-2 justify-content-end">
            <Link to="/reservations" className="btn btn-outline-secondary">Cancel</Link>
            <button type="submit" className="btn btn-primary" disabled={create.isPending}>
              {create.isPending ? "Saving…" : "Confirm Booking"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
