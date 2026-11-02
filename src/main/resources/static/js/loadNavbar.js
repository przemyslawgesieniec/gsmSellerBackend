// Shared feedback for data-heavy classic screens. It is deliberately delayed so
// quick requests do not flash, while slower reads always tell the user what is happening.
(() => {
    if (window.__pageLoadingIndicatorInstalled) return;
    window.__pageLoadingIndicatorInstalled = true;

    const originalFetch = window.fetch.bind(window);
    let activeRequests = 0;
    let revealTimer = null;

    const indicator = () => {
        let element = document.getElementById('pageLoadingIndicator');
        if (element) return element;

        element = document.createElement('div');
        element.id = 'pageLoadingIndicator';
        element.className = 'page-loading-indicator';
        element.hidden = true;
        element.setAttribute('role', 'status');
        element.setAttribute('aria-live', 'polite');
        element.setAttribute('aria-atomic', 'true');
        element.innerHTML = '<span class="page-loading-spinner" aria-hidden="true"></span><span>Pobieranie danych z systemu…</span>';
        document.body.append(element);
        return element;
    };

    const setLoading = loading => {
        const main = document.querySelector('main') || document.querySelector('.container');
        if (main) main.setAttribute('aria-busy', String(loading));
        indicator().hidden = !loading;
    };

    window.fetch = (input, options = {}) => {
        const method = (options.method || (input instanceof Request && input.method) || 'GET').toUpperCase();
        if (method !== 'GET') return originalFetch(input, options);

        activeRequests += 1;
        if (activeRequests === 1) {
            revealTimer = window.setTimeout(() => setLoading(true), 250);
        }

        return originalFetch(input, options).finally(() => {
            activeRequests = Math.max(0, activeRequests - 1);
            if (activeRequests !== 0) return;
            window.clearTimeout(revealTimer);
            revealTimer = null;
            setLoading(false);
        });
    };
})();

document.addEventListener("DOMContentLoaded", async function () {
    try {
        // 1️⃣ załaduj navbar HTML
        const response = await fetch("../navbar.html");
        const html = await response.text();
        document.getElementById("navbar").innerHTML = html;

        const currentPage = location.pathname.split('/').pop() || 'index.html';
        document.querySelectorAll('.new-workspace-link').forEach(link => {
            link.href = '/v2/' + currentPage + location.search;
        });

        // 2️⃣ init Materialize
        M.Sidenav.init(document.querySelectorAll('.sidenav'));
        M.Dropdown.init(document.querySelectorAll('#navbar .dropdown-trigger'), {
            hover: true,
            constrainWidth: false,
            coverTrigger: false,
            alignment: 'right'
        });

        // 3️⃣ 🔥 ZAŁADUJ UŻYTKOWNIKA ZAWSZE
        await loadCurrentUser();
        await refreshNotificationBadge();

    } catch (err) {
        console.error("Błąd ładowania navbaru:", err);
    }
});

async function refreshNotificationBadge() {
    try {
        const response = await fetch('/api/v1/notifications/unread-count');
        if (!response.ok) return;
        const count = Number(await response.json()) || 0;
        document.querySelectorAll('#notificationBadge, #notificationBadgeMobile').forEach(badge => {
            badge.style.display = count ? '' : 'none';
            badge.textContent = count > 99 ? '99+' : count;
        });
    } catch (_) {
        // The navigation must remain usable when notifications are temporarily unavailable.
    }
}
