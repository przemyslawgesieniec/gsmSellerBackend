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
