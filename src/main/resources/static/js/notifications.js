(() => {
    const list = document.getElementById('notificationList');
    const markAllButton = document.getElementById('markAllNotificationsRead');
    const escapeHtml = value => String(value ?? '').replace(/[&<>'"]/g, char => ({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[char]));
    const formatDate = value => new Intl.DateTimeFormat('pl-PL', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value));

    function reservationAction(item) {
        if (item.type !== 'RESERVATION_CREATED' || !item.reservationImei) return '';
        const href = `index.html?imei=${encodeURIComponent(item.reservationImei)}`;
        return `<a class="btn-small blue notification-stock-action" href="${href}" data-reservation-action data-notification-id="${item.id}" data-unread="${!item.readAt}"><i class="material-icons left">inventory_2</i>Przejdź do magazynu</a>`;
    }

    function render(items) {
        markAllButton.disabled = !items.some(item => !item.readAt);
        list.innerHTML = items.length ? items.map(item => `
          <article class="card-panel notification-row ${item.readAt ? 'grey lighten-4' : ''}" style="border-left:${item.readAt ? '4px solid #ddd' : '4px solid #00aed8'};">
            <button class="notification-main left-align" type="button" data-id="${item.id}">
            <strong>${escapeHtml(item.title)}</strong>${item.readAt ? '<span class="right grey-text">Przeczytane</span>' : '<span class="new badge right" data-badge-caption="Nowe"></span>'}
            <p class="grey-text text-darken-2" style="margin:8px 0;">${escapeHtml(item.content)}</p>
            <small class="grey-text">${formatDate(item.createdAt)}</small>
            </button>
            ${reservationAction(item)}
          </article>`).join('') : '<div class="card-panel center-align grey-text"><i class="material-icons medium">notifications_none</i><p>Brak powiadomień.</p></div>';
    }

    async function load() {
        try {
            const response = await fetch('/api/v1/notifications');
            if (!response.ok) throw new Error();
            render(await response.json());
        } catch (_) {
            list.innerHTML = '<div class="card-panel red-text">Nie udało się pobrać powiadomień.</div>';
        }
    }

    list.addEventListener('click', async event => {
        const reservationLink = event.target.closest('[data-reservation-action]');
        if (reservationLink) {
            event.preventDefault();
            if (reservationLink.dataset.loading === 'true') return;
            reservationLink.dataset.loading = 'true';
            reservationLink.setAttribute('aria-busy', 'true');
            try {
                if (reservationLink.dataset.unread === 'true') {
                    await fetch(`/api/v1/notifications/${reservationLink.dataset.notificationId}/read`, { method: 'PUT' });
                }
            } finally {
                window.location.assign(reservationLink.href);
            }
            return;
        }

        const item = event.target.closest('[data-id]');
        if (!item || item.closest('.notification-row')?.classList.contains('grey')) return;
        item.disabled = true;
        try {
            await fetch(`/api/v1/notifications/${item.dataset.id}/read`, { method: 'PUT' });
            await load();
            await refreshNotificationBadge();
        } finally { item.disabled = false; }
    });
    markAllButton.addEventListener('click', async () => {
        markAllButton.disabled = true;
        await fetch('/api/v1/notifications/read-all', { method: 'PUT' });
        await load();
        await refreshNotificationBadge();
    });
    document.addEventListener('DOMContentLoaded', load);
})();
