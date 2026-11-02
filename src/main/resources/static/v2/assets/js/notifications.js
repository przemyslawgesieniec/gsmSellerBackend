(() => {
    const list = document.getElementById('notificationList');
    const markAllButton = document.getElementById('markAllNotificationsRead');
    const dateFormat = new Intl.DateTimeFormat('pl-PL', { dateStyle: 'medium', timeStyle: 'short' });

    function render(items) {
        if (!items.length) {
            list.innerHTML = `<section class="surface empty-state"><i class="ui-icon">notifications</i><h2>Brak powiadomień</h2><p>Nowe rezerwacje telefonów pojawią się tutaj automatycznie.</p></section>`;
            UI.enhance(list);
            markAllButton.disabled = true;
            return;
        }
        markAllButton.disabled = !items.some(item => !item.readAt);
        list.innerHTML = items.map(item => `
            <button class="notification-item ${item.readAt ? 'is-read' : 'is-unread'}" type="button" data-notification-id="${item.id}" aria-label="${window.AppNotifications.escapeHtml(item.title)}. ${item.readAt ? 'Przeczytane' : 'Nieprzeczytane'}">
              <span class="notification-icon"><i class="ui-icon">notifications</i></span>
              <span class="notification-copy"><span class="notification-item-title">${window.AppNotifications.escapeHtml(item.title)}</span><span>${window.AppNotifications.escapeHtml(item.content)}</span><time datetime="${item.createdAt}">${dateFormat.format(new Date(item.createdAt))}</time></span>
              ${item.readAt ? '<span class="notification-read">Przeczytane</span>' : '<span class="notification-new">Nowe</span>'}
            </button>`).join('');
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
        const item = event.target.closest('[data-notification-id]');
        if (!item || item.classList.contains('is-read')) return;
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
