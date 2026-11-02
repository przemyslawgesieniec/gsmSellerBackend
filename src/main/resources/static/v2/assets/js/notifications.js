(() => {
    const list = document.getElementById('notificationList');
    const markAllButton = document.getElementById('markAllNotificationsRead');
    const dateFormat = new Intl.DateTimeFormat('pl-PL', { dateStyle: 'medium', timeStyle: 'short' });

    function reservationAction(item) {
        if (item.type !== 'RESERVATION_CREATED' || !item.reservationImei) return '';
        const href = `index.html?imei=${encodeURIComponent(item.reservationImei)}`;
        return `<a class="button-small notification-stock-action" href="${href}" data-reservation-action data-notification-id="${item.id}" data-unread="${!item.readAt}"><i class="ui-icon">inventory_2</i>Przejdź do magazynu</a>`;
    }

    function render(items) {
        if (!items.length) {
            list.innerHTML = `<section class="surface empty-state"><i class="ui-icon">notifications</i><h2>Brak powiadomień</h2><p>Nowe rezerwacje telefonów pojawią się tutaj automatycznie.</p></section>`;
            UI.enhance(list);
            markAllButton.disabled = true;
            return;
        }
        markAllButton.disabled = !items.some(item => !item.readAt);
        list.innerHTML = items.map(item => `
            <article class="notification-item ${item.readAt ? 'is-read' : 'is-unread'}">
              <button class="notification-main" type="button" data-notification-id="${item.id}" aria-label="${window.AppNotifications.escapeHtml(item.title)}. ${item.readAt ? 'Przeczytane' : 'Nieprzeczytane'}">
              <span class="notification-icon"><i class="ui-icon">notifications</i></span>
              <span class="notification-copy"><span class="notification-item-title">${window.AppNotifications.escapeHtml(item.title)}</span><span>${window.AppNotifications.escapeHtml(item.content)}</span><time datetime="${item.createdAt}">${dateFormat.format(new Date(item.createdAt))}</time></span>
              ${item.readAt ? '<span class="notification-read">Przeczytane</span>' : '<span class="notification-new">Nowe</span>'}
              </button>
              ${reservationAction(item)}
            </article>`).join('');
        UI.enhance(list);
    }

    async function load() {
        try {
            const response = await fetch('/api/v1/notifications');
            if (!response.ok) throw new Error();
            render(await response.json());
        } catch {
            list.innerHTML = '<section class="surface empty-state"><h2>Nie udało się pobrać powiadomień</h2><p>Odśwież stronę lub spróbuj ponownie za chwilę.</p></section>';
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
                    await window.AppNotifications.markRead(reservationLink.dataset.notificationId);
                }
            } finally {
                window.location.assign(reservationLink.href);
            }
            return;
        }

        const item = event.target.closest('[data-notification-id]');
        if (!item || item.closest('.notification-item')?.classList.contains('is-read')) return;
        item.disabled = true;
        try {
            await window.AppNotifications.markRead(item.dataset.notificationId);
            await load();
        } catch {
            UI.toast({ html: 'Nie udało się oznaczyć powiadomienia jako przeczytane.', classes: 'red' });
            item.disabled = false;
        }
    });

    markAllButton.addEventListener('click', async () => {
        markAllButton.disabled = true;
        try {
            await window.AppNotifications.markAllRead();
            await load();
        } catch {
            UI.toast({ html: 'Nie udało się oznaczyć powiadomień jako przeczytane.', classes: 'red' });
        }
    });

    document.addEventListener('DOMContentLoaded', load);
})();
