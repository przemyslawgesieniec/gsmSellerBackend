(() => {
    const pageSize = 50;
    const state = { page: 1, totalPages: 0, totalElements: 0 };
    const dateFormat = new Intl.DateTimeFormat('pl-PL', { dateStyle: 'medium', timeStyle: 'medium' });
    const typeLabels = {
        AUTHENTICATION_SUCCEEDED: 'Logowanie', AUTHENTICATION_FAILED: 'Nieudane logowanie', LOGOUT: 'Wylogowanie',
        PHONE_CREATED: 'Dodano telefon', PHONE_UPDATED: 'Zmieniono telefon', PHONE_MODEL_ASSIGNED: 'Przypisano model',
        PHONE_ACCEPTED_AT_LOCATION: 'Przyjęto telefon', PHONE_REMOVED: 'Usunięto telefon', PHONE_RESTORED: 'Przywrócono telefon',
        PHONE_REMOVED_FROM_LOCATION: 'Usunięto z lokalizacji', PHONE_HANDED_OVER: 'Wydano telefon', PHONE_SOLD: 'Sprzedano telefon',
        OFFER_CREATED: 'Utworzono ofertę', OFFER_UPDATED: 'Zmieniono ofertę', OFFER_REMOVED: 'Usunięto ofertę',
        PURCHASE_CREATED: 'Utworzono skup', PURCHASE_CLOSED: 'Zamknięto skup', PURCHASE_PRICE_AGREED: 'Uzgodniono cenę', PURCHASE_COMPLETED: 'Zakończono skup', PURCHASE_COMMENT_ADDED: 'Dodano komentarz do skupu',
        CART_ITEM_ADDED: 'Dodano do koszyka', CART_ITEM_UPDATED: 'Zmieniono koszyk', CART_ITEM_REMOVED: 'Usunięto z koszyka',
        RECEIPT_CREATED: 'Wystawiono dokument', RECEIPT_CANCELLED: 'Anulowano dokument', RECEIPT_EDITED: 'Zmieniono dokument',
        REPAIR_CREATED: 'Utworzono naprawę', REPAIR_UPDATED: 'Zmieniono naprawę', REPAIR_STATUS_CHANGED: 'Zmieniono status naprawy', REPAIR_RESTORED_TO_SHOP: 'Przywrócono do sklepu', REPAIR_NOTE_ADDED: 'Dodano notatkę', REPAIR_NOTE_REMOVED: 'Usunięto notatkę', REPAIR_CLIENT_CREATED: 'Dodano klienta serwisu', REPAIR_CLIENT_UPDATED: 'Zmieniono klienta serwisu', REPAIR_CLIENT_REMOVED: 'Usunięto klienta serwisu', REPAIR_SERVICE_POINT_CREATED: 'Dodano punkt serwisowy',
        RESERVATION_CREATED: 'Utworzono rezerwację', RESERVATION_CANCELLED: 'Anulowano rezerwację', RESERVATION_EXPIRED: 'Rezerwacja wygasła',
        LOCATION_CREATED: 'Dodano lokalizację', LOCATION_UPDATED: 'Zmieniono lokalizację', PHONE_MODEL_CREATED: 'Dodano model', PHONE_MODEL_UPDATED: 'Zmieniono model', PHONE_MODEL_REMOVED: 'Usunięto model',
        USER_REGISTERED: 'Zarejestrowano użytkownika', USER_PASSWORD_RESET: 'Zresetowano hasło', USER_ACTIVATION_CHANGED: 'Zmieniono aktywność użytkownika', USER_DYNAMIC_LOCATION_CHANGED: 'Zmieniono tryb lokalizacji', USER_LOCATION_CHANGED: 'Zmieniono lokalizację użytkownika', NOTIFICATIONS_MARKED_READ: 'Oznaczono powiadomienia', SYSTEM_MUTATION: 'Operacja systemowa'
    };

    const form = document.getElementById('eventLogFilters');
    const typeSelect = document.getElementById('eventType');
    const rows = document.getElementById('eventLogRows');
    const summary = document.getElementById('eventLogSummary');
    const error = document.getElementById('eventLogError');
    const empty = document.getElementById('eventLogEmpty');
    const table = document.getElementById('eventLogTableContainer');
    const pagination = document.getElementById('eventLogPagination');

    const clean = value => value == null || value === '' ? '—' : String(value);
    const labelForType = type => typeLabels[type] || String(type || '').replaceAll('_', ' ');
    const clearResults = () => rows.replaceChildren();

    function destinationFor(entry) {
        if (entry.eventType?.startsWith('REPAIR_') && entry.resourceTechnicalId) {
            return { href: `repair.html?highlight=${encodeURIComponent(entry.resourceTechnicalId)}`, label: 'Serwis' };
        }
        if (entry.eventType?.startsWith('RECEIPT_') && entry.resourceTechnicalId) {
            return { href: `receipts.html?receiptId=${encodeURIComponent(entry.resourceTechnicalId)}`, label: 'Dokument' };
        }
        if ((entry.eventType?.startsWith('PHONE_') || entry.eventType?.startsWith('CART_') || entry.eventType?.startsWith('RESERVATION_')) && entry.imei) {
            return { href: `index.html?imei=${encodeURIComponent(entry.imei)}`, label: 'Magazyn' };
        }
        return null;
    }

    function query() {
        const params = new URLSearchParams({ page: String(state.page), size: String(pageSize), sort: 'occurredAt,desc' });
        for (const [key, value] of new FormData(form).entries()) {
            if (!value) continue;
            params.set(key, key === 'from' || key === 'to' ? new Date(value).toISOString() : value);
        }
        return params;
    }

    function createCell(value, className) {
        const cell = document.createElement('td');
        if (className) cell.className = className;
        cell.textContent = clean(value);
        return cell;
    }

    function renderRows(entries) {
        clearResults();
        entries.forEach(entry => {
            const row = document.createElement('tr');
            const time = document.createElement('time');
            time.dateTime = entry.occurredAt;
            time.textContent = entry.occurredAt ? dateFormat.format(new Date(entry.occurredAt)) : '—';
            const timeCell = document.createElement('td'); timeCell.append(time);
            const typeCell = createCell(labelForType(entry.eventType));
            const outcomeCell = document.createElement('td');
            const outcome = document.createElement('span');
            outcome.className = `event-outcome event-outcome-${entry.outcome === 'SUCCESS' ? 'success' : 'failure'}`;
            outcome.textContent = entry.outcome === 'SUCCESS' ? 'Sukces' : 'Błąd';
            outcomeCell.append(outcome);
            row.append(timeCell, typeCell, outcomeCell, createCell(entry.actorUsername), createCell(entry.imei, 'event-imei'));
            row.append(createCell(entry.details || entry.requestPath));
            const actionCell = document.createElement('td');
            const destination = destinationFor(entry);
            if (destination) {
                const action = document.createElement('a');
                action.className = 'button-small event-log-link'; action.href = destination.href;
                action.textContent = destination.label;
                const icon = document.createElement('i'); icon.className = 'ui-icon'; icon.textContent = 'arrow_forward';
                action.append(icon); actionCell.append(action);
            } else {
                actionCell.textContent = '—';
            }
            row.append(actionCell);
            rows.append(row);
        });
    }

    function setPagination(pageData) {
        state.totalPages = pageData.totalPages || 0;
        state.totalElements = pageData.totalElements || 0;
        pagination.hidden = state.totalPages <= 1;
        document.getElementById('eventLogPrevious').disabled = state.page <= 1;
        document.getElementById('eventLogNext').disabled = state.page >= state.totalPages;
        document.getElementById('eventLogPageInfo').textContent = `Strona ${state.page} z ${state.totalPages}`;
        const pageNumber = document.getElementById('eventLogPageNumber');
        pageNumber.max = String(Math.max(1, state.totalPages));
        pageNumber.value = String(state.page);
    }

    async function loadTypes() {
        const response = await fetch('/api/v1/admin/event-log/types');
        if (!response.ok) throw new Error('Nie udało się pobrać typów zdarzeń.');
        const types = await response.json();
        types.forEach(type => {
            const option = document.createElement('option'); option.value = type; option.textContent = labelForType(type); typeSelect.append(option);
        });
    }

    async function loadEntries() {
        error.hidden = true;
        empty.hidden = true;
        table.hidden = false;
        try {
            const response = await fetch(`/api/v1/admin/event-log?${query()}`);
            if (response.status === 403) { location.replace('index.html'); return; }
            if (!response.ok) throw new Error('Nie udało się pobrać dziennika zdarzeń. Spróbuj ponownie za chwilę.');
            const data = await response.json();
            const entries = data.content || [];
            renderRows(entries);
            summary.textContent = entries.length ? `Znaleziono ${data.totalElements} ${data.totalElements === 1 ? 'zdarzenie' : 'zdarzeń'}.` : 'Nie znaleziono zdarzeń.';
            empty.hidden = entries.length > 0;
            table.hidden = entries.length === 0;
            setPagination(data);
            UI.enhance(table);
        } catch (loadError) {
            clearResults(); table.hidden = true; pagination.hidden = true; summary.textContent = 'Nie udało się wyświetlić wyników.';
            error.textContent = loadError.message; error.hidden = false;
        }
    }

    form.addEventListener('submit', event => { event.preventDefault(); state.page = 1; loadEntries(); });
    document.getElementById('clearEventLogFilters').addEventListener('click', () => { form.reset(); state.page = 1; loadEntries(); });
    document.getElementById('refreshEventLog').addEventListener('click', loadEntries);
    document.getElementById('eventLogPrevious').addEventListener('click', () => { if (state.page > 1) { state.page -= 1; loadEntries(); } });
    document.getElementById('eventLogNext').addEventListener('click', () => { if (state.page < state.totalPages) { state.page += 1; loadEntries(); } });
    document.getElementById('eventLogPageJump').addEventListener('submit', event => {
        event.preventDefault();
        const requestedPage = Number(document.getElementById('eventLogPageNumber').value);
        if (Number.isInteger(requestedPage) && requestedPage >= 1 && requestedPage <= state.totalPages && requestedPage !== state.page) {
            state.page = requestedPage;
            loadEntries();
        }
    });

    document.addEventListener('DOMContentLoaded', async () => {
        const user = await loadCurrentUser();
        if (!user || !IS_ADMIN) { location.replace('index.html'); return; }
        try {
            await loadTypes();
            await loadEntries();
        } catch (typesError) {
            error.textContent = typesError.message; error.hidden = false; summary.textContent = 'Nie udało się przygotować filtrów.';
        }
    });
})();
