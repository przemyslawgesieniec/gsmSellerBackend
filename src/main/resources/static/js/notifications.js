(() => {
    const list = document.getElementById('notificationList');
    const markAllButton = document.getElementById('markAllNotificationsRead');
    const escapeHtml = value => String(value ?? '').replace(/[&<>'"]/g, char => ({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[char]));
    const formatDate = value => new Intl.DateTimeFormat('pl-PL', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value));

    function render(items) {
        markAllButton.disabled = !items.some(item => !item.readAt);
        list.innerHTML = items.length ? items.map(item => `
          <button class="card-panel left-align notification-row ${item.readAt ? 'grey lighten-4' : ''}" type="button" data-id="${item.id}" style="display:block;width:100%;border-left:${item.readAt ? '4px solid #ddd' : '4px solid #00aed8'};cursor:pointer;">
            <strong>${escapeHtml(item.title)}</strong>${item.readAt ? '<span class="right grey-text">Przeczytane</span>' : '<span class="new badge right" data-badge-caption="Nowe"></span>'}
            <p class="grey-text text-darken-2" style="margin:8px 0;">${escapeHtml(item.content)}</p>
            <small class="grey-text">${formatDate(item.createdAt)}</small>
          </button>`).join('') : '<div class="card-panel center-align grey-text"><i class="material-icons medium">notifications_none</i><p>Brak powiadomień.</p></div>';
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
        const item = event.target.closest('[data-id]');
        if (!item || item.classList.contains('grey')) return;
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
