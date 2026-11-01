let currentPage = 0;
const pageSize = 10;
let currentFilters = {
    name: '',
    surname: '',
    phoneNumber: ''
};

document.addEventListener('DOMContentLoaded', function() {
    UI.Dialog.init(document.querySelectorAll('.dialog'));
    loadClients();

    document.getElementById('btnSearch').addEventListener('click', () => {
        currentFilters.name = document.getElementById('filterName').value;
        currentFilters.surname = document.getElementById('filterSurname').value;
        currentFilters.phoneNumber = document.getElementById('filterPhone').value;
        currentPage = 0;
        loadClients();
    });

    document.getElementById('btnClearFilters').addEventListener('click', () => {
        document.getElementById('filterName').value = '';
        document.getElementById('filterSurname').value = '';
        document.getElementById('filterPhone').value = '';
        currentFilters = { name: '', surname: '', phoneNumber: '' };
        currentPage = 0;
        loadClients();
    });

    document.getElementById('btnSaveClient').addEventListener('click', saveClient);
    document.getElementById('btnAddClient').addEventListener('click', openAddModal);
    document.getElementById('btnConfirmDelete').addEventListener('click', confirmDelete);
});

async function loadClients() {
    const tableBody = document.getElementById('clientsTableBody');
    tableBody.innerHTML = '<tr><td colspan="4" class="center-align">Ładowanie...</td></tr>';

    const params = new URLSearchParams({
        page: currentPage,
        size: pageSize,
        name: currentFilters.name,
        surname: currentFilters.surname,
        phoneNumber: currentFilters.phoneNumber,
        sortBy: 'surname',
        sortDir: 'ASC'
    });

    try {
        const response = await fetch(`/api/v1/repair-clients?${params.toString()}`);
        if (!response.ok) throw new Error('Błąd ładowania danych');

        const data = await response.json();
        renderTable(data.content);
        renderPagination(data);
    } catch (error) {
        console.error('Error:', error);
        tableBody.innerHTML = '<tr><td colspan="4" class="center-align red-text">Błąd ładowania danych!</td></tr>';
    }
}

function renderTable(clients) {
    const tableBody = document.getElementById('clientsTableBody');
    tableBody.innerHTML = '';

    if (clients.length === 0) {
        tableBody.innerHTML = '<tr><td colspan="4" class="center-align">Brak klientów spełniających kryteria.</td></tr>';
        return;
    }

    clients.forEach(client => {
        const tr = document.createElement('tr');
        const repairsQuery = `${client.name} ${client.surname} (${client.phoneNumber})`;
        const repairsUrl = `serviceHistory.html?query=${encodeURIComponent(repairsQuery)}`;
        tr.innerHTML = `
            <td>${client.name}</td>
            <td>${client.surname}</td>
            <td>${client.phoneNumber}</td>
            <td class="right-align">
                <a class="button-small green interactive interactive-light" href="${repairsUrl}" title="Pokaż naprawy klienta" aria-label="Pokaż naprawy klienta">
                    <i class="ui-icon">build</i>
                </a>
                <button class="button-small blue interactive interactive-light" onclick="openEditModal('${client.technicalId}', '${client.name}', '${client.surname}', '${client.phoneNumber}')">
                    <i class="ui-icon">edit</i>
                </button>
                <button class="button-small red interactive interactive-light" onclick="openDeleteModal('${client.technicalId}', '${client.name} ${client.surname}')">
                    <i class="ui-icon">delete</i>
                </button>
            </td>
        `;
        tableBody.appendChild(tr);
    });
}

function renderPagination(data) {
    const container = document.getElementById('pagination');
    container.innerHTML = '';

    if (data.totalPages <= 1) return;

    const ul = document.createElement('ul');
    ul.className = 'pagination';

    // Prev
    const prevLi = document.createElement('li');
    prevLi.className = data.first ? 'disabled' : 'interactive';
    prevLi.innerHTML = `<a href="#!"><i class="ui-icon">chevron_left</i></a>`;
    if (!data.first) {
        prevLi.onclick = () => { currentPage--; loadClients(); };
    }
    ul.appendChild(prevLi);

    // Pages
    for (let i = 0; i < data.totalPages; i++) {
        const li = document.createElement('li');
        li.className = i === currentPage ? 'active blue' : 'interactive';
        li.innerHTML = `<a href="#!">${i + 1}</a>`;
        li.onclick = () => { currentPage = i; loadClients(); };
        ul.appendChild(li);
    }

    // Next
    const nextLi = document.createElement('li');
    nextLi.className = data.last ? 'disabled' : 'interactive';
    nextLi.innerHTML = `<a href="#!"><i class="ui-icon">chevron_right</i></a>`;
    if (!data.last) {
        nextLi.onclick = () => { currentPage++; loadClients(); };
    }
    ul.appendChild(nextLi);

    container.appendChild(ul);
}

function openAddModal() {
    document.getElementById('modalTitle').innerText = 'Dodaj nowego klienta';
    document.getElementById('clientTechnicalId').value = '';
    document.getElementById('modalClientName').value = '';
    document.getElementById('modalClientSurname').value = '';
    document.getElementById('modalClientPhone').value = '';
    UI.updateTextFields();
}

function openEditModal(id, name, surname, phone) {
    document.getElementById('modalTitle').innerText = 'Edytuj klienta';
    document.getElementById('clientTechnicalId').value = id;
    document.getElementById('modalClientName').value = name;
    document.getElementById('modalClientSurname').value = surname;
    document.getElementById('modalClientPhone').value = phone;
    UI.updateTextFields();
    const instance = UI.Dialog.getInstance(document.getElementById('clientModal'));
    instance.open();
}

async function saveClient() {
    const id = document.getElementById('clientTechnicalId').value;
    const clientData = {
        name: document.getElementById('modalClientName').value,
        surname: document.getElementById('modalClientSurname').value,
        phoneNumber: document.getElementById('modalClientPhone').value
    };

    if (!clientData.name || !clientData.surname || !clientData.phoneNumber) {
        UI.toast({html: 'Proszę wypełnić wszystkie wymagane pola', classes: 'red'});
        return;
    }

    const url = id ? `/api/v1/repair-clients/${id}` : '/api/v1/repair-clients';
    const method = id ? 'PUT' : 'POST';

    try {
        const response = await fetch(url, {
            method: method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(clientData)
        });

        if (response.ok) {
            UI.toast({html: id ? 'Zaktualizowano klienta' : 'Dodano nowego klienta', classes: 'green'});
            UI.Dialog.getInstance(document.getElementById('clientModal')).close();
            loadClients();
        } else {
            throw new Error('Błąd zapisu');
        }
    } catch (error) {
        console.error('Error:', error);
        UI.toast({html: 'Wystąpił błąd podczas zapisu', classes: 'red'});
    }
}

let clientIdToDelete = null;

function openDeleteModal(id, info) {
    clientIdToDelete = id;
    document.getElementById('deleteClientInfo').innerText = info;
    UI.Dialog.getInstance(document.getElementById('deleteConfirmModal')).open();
}

async function confirmDelete() {
    if (!clientIdToDelete) return;

    try {
        const response = await fetch(`/api/v1/repair-clients/${clientIdToDelete}`, {
            method: 'DELETE'
        });

        if (response.ok) {
            UI.toast({html: 'Usunięto klienta', classes: 'green'});
            UI.Dialog.getInstance(document.getElementById('deleteConfirmModal')).close();
            loadClients();
        } else {
            throw new Error('Błąd usuwania');
        }
    } catch (error) {
        console.error('Error:', error);
        UI.toast({html: 'Nie można usunąć klienta (prawdopodobnie ma powiązane naprawy)', classes: 'red'});
    } finally {
        clientIdToDelete = null;
    }
}
