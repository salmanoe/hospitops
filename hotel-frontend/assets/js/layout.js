// ═══════════════════════════════════════════════════════════════
// assets/js/layout.js
// Include AFTER core.js on every page that has a sidebar.
// Loads the sidebar component and wires up navigation.
// ═══════════════════════════════════════════════════════════════

(async function loadLayout() {
    // Guard — redirect to login if not authenticated
    if (!Auth.isLoggedIn()) {
        window.location.href = '/login.html';
        return;
    }

    // Fetch sidebar HTML
    const res  = await fetch('/components/sidebar.html');
    const html = await res.text();

    // Inject into #sidebar placeholder
    const sidebar = document.getElementById('sidebar');
    if (!sidebar) return;
    sidebar.innerHTML = html;

    // Re-execute inline <script> blocks inside the fetched HTML
    // (innerHTML does not execute scripts automatically)
    sidebar.querySelectorAll('script').forEach(oldScript => {
        const newScript = document.createElement('script');
        newScript.textContent = oldScript.textContent;
        document.head.appendChild(newScript);
        oldScript.remove();
    });

    // Mobile sidebar toggle — only wire up on narrow viewports
    if (window.innerWidth <= 768) {
        const topbar = document.querySelector('.topbar');
        if (topbar) {
            // Inject hamburger as the first child of the topbar so it sits
            // flush left beside the page title, rather than floating over content.
            const toggle = document.createElement('button');
            toggle.className = 'sidebar-toggle';
            toggle.setAttribute('aria-label', 'Toggle navigation');
            toggle.setAttribute('aria-expanded', 'false');
            toggle.innerHTML = '&#9776;'; // ☰
            topbar.prepend(toggle);

            // Semi-transparent backdrop — tapping it closes the sidebar.
            const overlay = document.createElement('div');
            overlay.id = 'sidebar-overlay';
            document.body.prepend(overlay);

            const openSidebar = () => {
                sidebar.classList.add('open');
                overlay.classList.add('show');
                toggle.setAttribute('aria-expanded', 'true');
                document.body.style.overflow = 'hidden'; // prevent scroll behind overlay
            };

            const closeSidebar = () => {
                sidebar.classList.remove('open');
                overlay.classList.remove('show');
                toggle.setAttribute('aria-expanded', 'false');
                document.body.style.overflow = '';
            };

            toggle.addEventListener('click', () =>
                sidebar.classList.contains('open') ? closeSidebar() : openSidebar());

            overlay.addEventListener('click', closeSidebar);

            // Close sidebar when the user taps any nav link (so the page
            // navigates cleanly without the sidebar left hanging open).
            sidebar.addEventListener('click', (e) => {
                if (e.target.closest('.nav-link')) {
                    // Small delay lets the browser start the navigation first.
                    setTimeout(closeSidebar, 120);
                }
            });
        }
    }
})();
