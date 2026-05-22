// ═══════════════════════════════════════════════════════════════
// assets/js/core.js
// R19 FIX: Updated comment — all three modules are in this single file.
//
// Contains three IIFE modules, all accessible as globals:
//   Auth  — JWT storage, session management, role-based access control
//   API   — centralized fetch wrapper, injects Authorization header,
//           handles 401 redirect, provides domain-specific API methods
//   Utils — formatters (Rp, date, badge), DOM helpers, toast notifications,
//           pagination renderer, form helpers, debounce, PDF download
//
// Load order matters: Auth must be defined before API (API uses Auth.getToken()).
// This file must be loaded BEFORE layout.js on every authenticated page.
// ═══════════════════════════════════════════════════════════════


// ═══════════════════════════════════════════════════════════════
// assets/js/auth.js
// JWT storage, session management, role-based access control
// ═══════════════════════════════════════════════════════════════

const Auth = (() => {
    const TOKEN_KEY = 'hospitops_token';
    const USER_KEY  = 'hospitops_user';

    const save = (loginResponse) => {
        localStorage.setItem(TOKEN_KEY, loginResponse.token);
        localStorage.setItem(USER_KEY, JSON.stringify({
            id:       loginResponse.staffId,
            name:     loginResponse.fullName,
            username: loginResponse.username,
            role:     loginResponse.role,
        }));
    };

    const clear = () => {
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(USER_KEY);
    };

    const getToken = () => localStorage.getItem(TOKEN_KEY);

    const getUser = () => {
        const raw = localStorage.getItem(USER_KEY);
        return raw ? JSON.parse(raw) : null;
    };

    const isLoggedIn = () => !!getToken() && !!getUser();

    const hasRole = (...roles) => {
        const user = getUser();
        return user && roles.includes(user.role);
    };

    // R21 FIX: Removed isAtLeast() — it implied a linear role hierarchy that
    // doesn't exist. ACCOUNTANT and FRONT_DESK are parallel roles; neither
    // outranks the other. Always use hasRole() with explicit role lists:
    //   Auth.hasRole('ADMIN', 'MANAGER', 'FRONT_DESK')
    // never:
    //   Auth.isAtLeast('FRONT_DESK')  ← misleading, removed

    // Guard: redirect to login if not authenticated
    const requireAuth = () => {
        if (!isLoggedIn()) {
            window.location.href = '/login.html';
            return false;
        }
        return true;
    };

    // Guard: redirect to dashboard if not authorized
    const requireRole = (...roles) => {
        if (!requireAuth()) return false;
        if (!hasRole(...roles)) {
            window.location.href = '/dashboard.html';
            return false;
        }
        return true;
    };

    // Show/hide elements based on role
    const applyRoleVisibility = () => {
        const user = getUser();
        if (!user) return;

        document.querySelectorAll('[data-roles]').forEach(el => {
            const allowed = el.dataset.roles.split(',').map(r => r.trim());
            el.style.display = allowed.includes(user.role) ? '' : 'none';
        });
    };

    return { save, clear, getToken, getUser, isLoggedIn,
             hasRole, requireAuth, requireRole,    // R21: isAtLeast removed
             applyRoleVisibility };
})();


// ═══════════════════════════════════════════════════════════════
// assets/js/api.js
// Centralized fetch wrapper — injects JWT, handles errors
// ═══════════════════════════════════════════════════════════════

const API = (() => {
    const BASE = '/api/v1';   // same origin — nginx proxies to backend

    const headers = (extra = {}) => ({
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${Auth.getToken() || ''}`,
        ...extra,
    });

    const handle = async (response) => {
        if (response.status === 401) {
            Auth.clear();
            window.location.href = '/login.html';
            throw new Error('Unauthorized');
        }
        const body = await response.json().catch(() => ({}));
        if (!response.ok) {
            throw new ApiError(response.status, body.message || 'Request failed', body.error);
        }
        return body.data !== undefined ? body.data : body;
    };

    const get = (path, params = {}) => {
        const qs = new URLSearchParams(params).toString();
        return fetch(`${BASE}${path}${qs ? '?' + qs : ''}`, { headers: headers() })
            .then(handle);
    };

    const post = (path, body) =>
        fetch(`${BASE}${path}`, {
            method: 'POST',
            headers: headers(),
            body: JSON.stringify(body),
        }).then(handle);

    const put = (path, body) =>
        fetch(`${BASE}${path}`, {
            method: 'PUT',
            headers: headers(),
            body: JSON.stringify(body),
        }).then(handle);

    const patch = (path, body = {}) =>
        fetch(`${BASE}${path}`, {
            method: 'PATCH',
            headers: headers(),
            body: JSON.stringify(body),
        }).then(handle);

    const del = (path) =>
        fetch(`${BASE}${path}`, { method: 'DELETE', headers: headers() })
            .then(handle);

    // PDF download — returns blob
    const getPdf = async (path) => {
        const response = await fetch(`${BASE}${path}`, { headers: headers() });
        if (!response.ok) throw new Error('PDF download failed');
        return response.blob();
    };

    // ── Domain-specific API calls ────────────────────────────
    return {
        // Auth
        auth: {
            login:  (username, password) => post('/auth/login', { username, password }),
            logout: () => post('/auth/logout', {}),
            me:     () => get('/auth/me'),
        },

        // Rooms
        rooms: {
            list:      (params) => get('/rooms', params),
            get:       (id)     => get(`/rooms/${id}`),
            create:    (data)   => post('/rooms', data),
            update:    (id, d)  => put(`/rooms/${id}`, d),
            available: (checkIn, checkOut) => get('/rooms/available', { checkIn, checkOut }),
        },

        roomTypes: {
            list:      (params) => get('/room-types', params),
            get:       (id)     => get(`/room-types/${id}`),
            create:    (data)   => post('/room-types', data),
            update:    (id, d)  => put(`/room-types/${id}`, d),
            addRate:   (id, d)  => post(`/room-types/${id}/rates`, d),
        },

        // Guests
        guests: {
            list:        (params) => get('/guests', params),
            get:         (id)     => get(`/guests/${id}`),
            search:      (q)      => get('/guests/search', { q }),
            register:    (data)   => post('/guests', data),
            update:      (id, d)  => put(`/guests/${id}`, d),
        },

        // Reservations
        reservations: {
            list:       (params) => get('/reservations', params),
            get:        (id)     => get(`/reservations/${id}`),
            create:     (data)   => post('/reservations', data),
            checkIn:    (id)     => patch(`/reservations/${id}/checkin`),
            checkOut:   (id)     => patch(`/reservations/${id}/checkout`),
            cancel:     (id)     => patch(`/reservations/${id}/cancel`),
            arrivals:   ()       => get('/reservations/today/arrivals'),
            departures: ()       => get('/reservations/today/departures'),
        },

        // Housekeeping
        housekeeping: {
            board:            ()           => get('/housekeeping/board'),
            tasks:            (params)     => get('/housekeeping/tasks', params),
            createTask:       (data)       => post('/housekeeping/tasks', data),
            assign:           (id, staffId) => patch(`/housekeeping/tasks/${id}/assign`, { staffId }),
            complete:         (id)         => patch(`/housekeeping/tasks/${id}/complete`),
            updateRoomStatus: (id, data)   => patch(`/housekeeping/rooms/${id}/status`, data),
        },

        // Billing
        invoices: {
            list:          (params) => get('/invoices', params),
            get:           (id)     => get(`/invoices/${id}`),
            recordPayment: (id, d)  => post(`/invoices/${id}/payments`, d),
            pdf:           (id)     => getPdf(`/invoices/${id}/pdf`),
        },

        // Staff
        staff: {
            list:           (params) => get('/staff', params),
            get:            (id)     => get(`/staff/${id}`),
            create:         (data)   => post('/staff', data),
            update:         (id, d)  => put(`/staff/${id}`, d),
            changePassword: (id, d)  => patch(`/staff/${id}/password`, d),
            toggle:         (id)     => patch(`/staff/${id}/toggle`),
        },

        // expose raw methods for edge cases
        get, post, put, patch, del, getPdf,
    };
})();

class ApiError extends Error {
    constructor(status, message, details) {
        super(message);
        this.status  = status;
        this.details = details;
    }
}


// ═══════════════════════════════════════════════════════════════
// assets/js/utils.js
// Shared formatting, DOM helpers, toast notifications
// ═══════════════════════════════════════════════════════════════

const Utils = (() => {

    // ── Formatters ───────────────────────────────────────────
    const formatRp = (amount) => {
        if (amount == null) return '—';
        return 'Rp ' + Number(amount).toLocaleString('id-ID', {
            minimumFractionDigits: 0, maximumFractionDigits: 0
        });
    };

    const formatDate = (dateStr) => {
        if (!dateStr) return '—';
        return new Date(dateStr).toLocaleDateString('id-ID', {
            day: '2-digit', month: 'short', year: 'numeric'
        });
    };

    const formatDateTime = (dateStr) => {
        if (!dateStr) return '—';
        return new Date(dateStr).toLocaleString('id-ID', {
            day: '2-digit', month: 'short', year: 'numeric',
            hour: '2-digit', minute: '2-digit'
        });
    };

    const formatNights = (n) => `${n} night${n !== 1 ? 's' : ''}`;

    // ── Badge helpers ────────────────────────────────────────
    // R-19 FIX: status text is now HTML-escaped before interpolation into
    // innerHTML to prevent XSS if an unexpected value arrives from the API.
    const _escHtml = (s) => String(s)
        .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;').replace(/'/g, '&#39;');

    const statusBadge = (status) => {
        const map = {
            AVAILABLE:   'success',  OCCUPIED:    'warning',
            DIRTY:       'secondary',MAINTENANCE: 'danger',
            CONFIRMED:   'info',     CHECKED_IN:  'warning',
            CHECKED_OUT: 'success',  CANCELLED:   'danger',
            PENDING:     'secondary',
            UNPAID:      'danger',   PARTIAL:     'warning',  PAID: 'success',
        };
        const color = map[status] || 'secondary';
        // Replace ALL underscores (not just first) and escape for safe innerHTML
        return `<span class="badge bg-${color}">${_escHtml(String(status).replace(/_/g, ' '))}</span>`;
    };

    // ── DOM helpers ──────────────────────────────────────────
    const qs  = (sel, ctx = document) => ctx.querySelector(sel);
    const qsa = (sel, ctx = document) => [...ctx.querySelectorAll(sel)];

    const show = (el) => { if (el) el.classList.remove('d-none'); };
    const hide = (el) => { if (el) el.classList.add('d-none'); };

    const setHtml = (sel, html, ctx = document) => {
        const el = qs(sel, ctx);
        if (el) el.innerHTML = html;
    };

    const setText = (sel, text, ctx = document) => {
        const el = qs(sel, ctx);
        if (el) el.textContent = text ?? '—';
    };

    // ── Toast notifications ──────────────────────────────────
    const toast = (message, type = 'success') => {
        const container = document.getElementById('toast-container')
            || (() => {
                const d = document.createElement('div');
                d.id = 'toast-container';
                d.className = 'toast-container position-fixed bottom-0 end-0 p-3';
                d.style.zIndex = 9999;
                document.body.appendChild(d);
                return d;
            })();

        const id   = 'toast-' + Date.now();
        const html = `
            <div id="${id}" class="toast align-items-center text-bg-${type} border-0" role="alert">
                <div class="d-flex">
                    <div class="toast-body fw-semibold">${message}</div>
                    <button type="button" class="btn-close btn-close-white me-2 m-auto"
                            data-bs-dismiss="toast"></button>
                </div>
            </div>`;
        container.insertAdjacentHTML('beforeend', html);
        const el = document.getElementById(id);
        new bootstrap.Toast(el, { delay: 3500 }).show();
        el.addEventListener('hidden.bs.toast', () => el.remove());
    };

    // ── Loading state ────────────────────────────────────────
    const loading = (btnEl, isLoading) => {
        if (!btnEl) return;
        if (isLoading) {
            btnEl.dataset.originalText = btnEl.innerHTML;
            btnEl.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Loading…';
            btnEl.disabled = true;
        } else {
            btnEl.innerHTML = btnEl.dataset.originalText || btnEl.innerHTML;
            btnEl.disabled  = false;
        }
    };

    // ── Pagination renderer ──────────────────────────────────
    const renderPagination = (containerId, pageResult, onPageChange) => {
        const el = document.getElementById(containerId);
        if (!el || pageResult.totalPages <= 1) {
            if (el) el.innerHTML = '';
            return;
        }
        const items = [];
        for (let i = 0; i < pageResult.totalPages; i++) {
            items.push(`
                <li class="page-item ${i === pageResult.page ? 'active' : ''}">
                    <button class="page-link" data-page="${i}">${i + 1}</button>
                </li>`);
        }
        el.innerHTML = `<ul class="pagination pagination-sm mb-0">${items.join('')}</ul>`;
        el.querySelectorAll('[data-page]').forEach(btn =>
            btn.addEventListener('click', () => onPageChange(+btn.dataset.page)));
    };

    // ── Form helpers ─────────────────────────────────────────
    // R-20 FIX: use strict empty-string check instead of truthiness test.
    // `v || null` coerced "0", "false", and other falsy-but-valid strings to null.
    // `v !== '' ? v : null` preserves those values while still treating an
    // unfilled text input (empty string) as absent.
    const formData = (formEl) => {
        const data = {};
        new FormData(formEl).forEach((v, k) => { data[k] = v !== '' ? v : null; });
        return data;
    };

    const setFormErrors = (errors) => {
        // Clear previous errors
        document.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
        document.querySelectorAll('.invalid-feedback').forEach(el => el.remove());

        if (typeof errors === 'object') {
            Object.entries(errors).forEach(([field, msg]) => {
                const input = document.querySelector(`[name="${field}"]`);
                if (input) {
                    input.classList.add('is-invalid');
                    input.insertAdjacentHTML('afterend',
                        `<div class="invalid-feedback">${msg}</div>`);
                }
            });
        }
    };

    // ── URL helpers ──────────────────────────────────────────
    const getParam = (name) => new URLSearchParams(location.search).get(name);

    // ── Debounce ─────────────────────────────────────────────
    const debounce = (fn, ms = 300) => {
        let t;
        return (...args) => { clearTimeout(t); t = setTimeout(() => fn(...args), ms); };
    };

    // ── PDF download helper ──────────────────────────────────
    const downloadPdf = async (blob, filename) => {
        const url = URL.createObjectURL(blob);
        const a   = document.createElement('a');
        a.href = url; a.download = filename;
        document.body.appendChild(a); a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
    };

    return {
        formatRp, formatDate, formatDateTime, formatNights,
        statusBadge, qs, qsa, show, hide, setHtml, setText,
        toast, loading, renderPagination, formData, setFormErrors,
        getParam, debounce, downloadPdf,
    };
})();
