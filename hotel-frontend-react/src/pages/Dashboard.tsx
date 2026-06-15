import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  Cell,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { api } from "../lib/api";
import { formatRp, statusColor, statusLabel } from "../lib/utils";
import type { Invoice, ReservationSummary } from "../lib/types";

const TREND_DAYS = 14;
const BRASS = "#a9824c";

const addDays = (d: Date, n: number) => {
  const x = new Date(d);
  x.setDate(x.getDate() + n);
  return x;
};
const iso = (d: Date) => d.toISOString().slice(0, 10);
const dayLabel = (d: Date) => `${d.getDate()}/${d.getMonth() + 1}`;

const ACTIVE: ReservationSummary["status"][] = ["PENDING", "CONFIRMED", "CHECKED_IN"];

// Bootstrap contextual colour → hex, so recharts slices match the status badges.
const STATUS_HEX: Record<string, string> = {
  success: "#198754",
  warning: "#ffc107",
  danger: "#dc3545",
  info: "#0dcaf0",
  secondary: "#6c757d",
};
const statusHex = (status: string) => STATUS_HEX[statusColor(status)] ?? "#6c757d";

function KpiCard({ label, value, hint }: { label: string; value: string; hint?: string }) {
  return (
    <div className="col-6 col-lg-3">
      <div className="card p-3 h-100">
        <div className="text-muted small text-uppercase">{label}</div>
        <div className="fs-4 fw-bold">{value}</div>
        {hint ? <small className="text-muted">{hint}</small> : null}
      </div>
    </div>
  );
}

export default function Dashboard() {
  const today = new Date().toLocaleDateString("en-GB", {
    weekday: "long", day: "2-digit", month: "long", year: "numeric",
  });

  // Trailing 30-day window for realised ADR / RevPAR / occupancy.
  const metricsFrom = iso(addDays(new Date(), -30));
  const metricsTo = iso(new Date());

  const roomsQ = useQuery({ queryKey: ["rooms", { size: 200 }], queryFn: () => api.rooms.list({ size: 200 }) });
  const resQ = useQuery({ queryKey: ["reservations", "analytics", { size: 500 }], queryFn: () => api.reservations.list({ size: 500 }) });
  const invQ = useQuery({ queryKey: ["invoices", "analytics", { size: 500 }], queryFn: () => api.invoices.list({ size: 500 }) });
  const metricsQ = useQuery({
    queryKey: ["reservations", "revenue-metrics", metricsFrom, metricsTo],
    queryFn: () => api.reservations.revenueMetrics(metricsFrom, metricsTo),
  });
  const metrics = metricsQ.data;

  const rooms = roomsQ.data?.content ?? [];
  const reservations = useMemo(() => resQ.data?.content ?? [], [resQ.data]);
  const invoices: Invoice[] = useMemo(() => invQ.data?.content ?? [], [invQ.data]);

  const totalRooms = rooms.length;
  const occupied = rooms.filter((r) => r.status === "OCCUPIED").length;
  const available = rooms.filter((r) => r.status === "AVAILABLE").length;
  const occupancyRate = totalRooms ? Math.round((occupied / totalRooms) * 100) : 0;

  // ── Month-to-date revenue from invoices ──────────────────────────────────
  const monthKey = iso(new Date()).slice(0, 7); // YYYY-MM
  const monthInvoices = useMemo(
    () => invoices.filter((i) => (i.issuedAt ?? "").slice(0, 7) === monthKey),
    [invoices, monthKey],
  );
  const monthRevenue = monthInvoices.reduce((s, i) => s + (i.totalAmount ?? 0), 0);
  const outstanding = invoices.reduce((s, i) => s + (i.balance ?? 0), 0);

  // ── Revenue, last 14 days (bar) ──────────────────────────────────────────
  const revenueSeries = useMemo(() => {
    const map = new Map<string, number>();
    for (const inv of invoices) {
      const d = (inv.issuedAt ?? "").slice(0, 10);
      if (d) map.set(d, (map.get(d) ?? 0) + (inv.totalAmount ?? 0));
    }
    const start = addDays(new Date(), -(TREND_DAYS - 1));
    return Array.from({ length: TREND_DAYS }, (_, i) => {
      const d = addDays(start, i);
      return { day: dayLabel(d), revenue: map.get(iso(d)) ?? 0 };
    });
  }, [invoices]);

  // ── Occupancy forecast, next 14 days (area) ──────────────────────────────
  const occupancySeries = useMemo(() => {
    const start = new Date();
    start.setHours(0, 0, 0, 0);
    return Array.from({ length: TREND_DAYS }, (_, i) => {
      const d = iso(addDays(start, i));
      const count = reservations.filter(
        (r) => ACTIVE.includes(r.status) && r.checkInDate && r.checkOutDate && r.checkInDate <= d && d < r.checkOutDate,
      ).length;
      return { day: dayLabel(addDays(start, i)), occupancy: totalRooms ? Math.round((count / totalRooms) * 100) : 0 };
    });
  }, [reservations, totalRooms]);

  // ── Reservations by status (pie) ─────────────────────────────────────────
  const statusSeries = useMemo(() => {
    const map = new Map<string, number>();
    for (const r of reservations) map.set(r.status, (map.get(r.status) ?? 0) + 1);
    return Array.from(map.entries()).map(([status, value]) => ({ status, name: statusLabel(status), value }));
  }, [reservations]);

  const loading = roomsQ.isLoading || invQ.isLoading || resQ.isLoading;
  const dash = loading ? "—" : undefined;

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center px-4 py-3 bg-white border-bottom">
        <h2 className="h4 mb-0">Dashboard</h2>
        <span className="text-muted small">{today}</span>
      </div>

      <div className="p-4">
        <div className="row g-3 mb-4">
          <KpiCard label="Occupancy" value={dash ?? `${occupancyRate}%`} hint={`${occupied}/${totalRooms} rooms occupied`} />
          <KpiCard label="Available" value={dash ?? String(available)} hint="Ready to book" />
          <KpiCard label="Revenue (MTD)" value={dash ?? formatRp(monthRevenue)} hint={`${monthInvoices.length} invoices this month`} />
          <KpiCard label="Outstanding" value={dash ?? formatRp(outstanding)} hint="Unpaid balances" />
        </div>

        <div className="row g-3 mb-4">
          <KpiCard label="ADR" value={metricsQ.isLoading ? "—" : formatRp(metrics?.adr ?? 0)} hint="Avg daily rate · last 30d" />
          <KpiCard label="RevPAR" value={metricsQ.isLoading ? "—" : formatRp(metrics?.revpar ?? 0)} hint="Revenue per available room · 30d" />
          <KpiCard label="Occupancy (30d)" value={metricsQ.isLoading ? "—" : `${metrics?.occupancyRate ?? 0}%`} hint={`${metrics?.roomNightsSold ?? 0} of ${metrics?.availableRoomNights ?? 0} room-nights`} />
          <KpiCard label="In-House" value={dash ?? String(reservations.filter((r) => r.status === "CHECKED_IN").length)} hint="Currently checked in" />
        </div>

        <ChartsSection
          revenueSeries={revenueSeries}
          occupancySeries={occupancySeries}
          statusSeries={statusSeries}
        />
      </div>
    </div>
  );
}

// Charts grouped together; kept as a small component so the data wiring above stays readable.
function ChartsSection({
  revenueSeries,
  occupancySeries,
  statusSeries,
}: {
  revenueSeries: { day: string; revenue: number }[];
  occupancySeries: { day: string; occupancy: number }[];
  statusSeries: { status: string; name: string; value: number }[];
}) {
  const rpAxis = (v: number) => (v >= 1_000_000 ? `${(v / 1_000_000).toFixed(0)}M` : v >= 1000 ? `${(v / 1000).toFixed(0)}k` : String(v));

  return (
    <div>
      <div className="row g-3">
        <div className="col-12 col-xl-7">
          <div className="card p-3 h-100">
            <div className="text-muted small text-uppercase mb-2">Revenue — last 14 days</div>
            <ResponsiveContainer width="100%" height={260}>
              <BarChart data={revenueSeries} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
                <XAxis dataKey="day" fontSize={11} tickLine={false} />
                <YAxis tickFormatter={rpAxis} fontSize={11} tickLine={false} width={40} />
                <Tooltip formatter={(v) => formatRp(Number(v))} />
                <Bar dataKey="revenue" fill={BRASS} radius={[3, 3, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        <div className="col-12 col-xl-5">
          <div className="card p-3 h-100">
            <div className="text-muted small text-uppercase mb-2">Reservations by status</div>
            <ResponsiveContainer width="100%" height={260}>
              <PieChart>
                <Pie data={statusSeries} dataKey="value" nameKey="name" innerRadius={55} outerRadius={90} paddingAngle={2}>
                  {statusSeries.map((s) => (
                    <Cell key={s.status} fill={statusHex(s.status)} />
                  ))}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
            <div className="d-flex flex-wrap gap-2 justify-content-center small">
              {statusSeries.map((s) => (
                <span key={s.status} className="d-inline-flex align-items-center gap-1">
                  <span style={{ width: 10, height: 10, borderRadius: 2, background: statusHex(s.status), display: "inline-block" }} />
                  {s.name} ({s.value})
                </span>
              ))}
            </div>
          </div>
        </div>

        <div className="col-12">
          <div className="card p-3">
            <div className="text-muted small text-uppercase mb-2">Occupancy forecast — next 14 days</div>
            <ResponsiveContainer width="100%" height={240}>
              <AreaChart data={occupancySeries} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
                <defs>
                  <linearGradient id="occFill" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor={BRASS} stopOpacity={0.45} />
                    <stop offset="100%" stopColor={BRASS} stopOpacity={0.05} />
                  </linearGradient>
                </defs>
                <XAxis dataKey="day" fontSize={11} tickLine={false} />
                <YAxis tickFormatter={(v: number) => `${v}%`} domain={[0, 100]} fontSize={11} tickLine={false} width={40} />
                <Tooltip formatter={(v) => `${Number(v)}%`} />
                <Area type="monotone" dataKey="occupancy" stroke={BRASS} strokeWidth={2} fill="url(#occFill)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>
    </div>
  );
}
