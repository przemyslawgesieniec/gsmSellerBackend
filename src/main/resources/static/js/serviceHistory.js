let currentPage = 0;
const pageSize = 10;
let receiptPicker, handoverPicker;
let selectedClientsMap = {};

document.addEventListener('DOMContentLoaded', function() {
    M.Collapsible.init(document.querySelectorAll('.collapsible'));
    initDatePickers();
    initClientAutocomplete();
    
    // Obsługa parametru query z URL
    const urlParams = new URLSearchParams(window.location.search);
    const queryParam = urlParams.get('query');
    if (queryParam) {
        document.getElementById('historySearch').value = queryParam;
        M.updateTextFields();
    }
    
    loadHistory();

    document.getElementById('clearFiltersBtn').addEventListener('click', () => {
        document.getElementById('historySearch').value = '';
        if (receiptPicker) receiptPicker.clear();
        if (handoverPicker) handoverPicker.clear();
        currentPage = 0;
        loadHistory();
    });
});

function debounce(func, wait) {
    let timeout;
    return function() {
        const context = this, args = arguments;
        clearTimeout(timeout);
        timeout = setTimeout(() => func.apply(context, args), wait);
    };
}

function initClientAutocomplete() {
    const el = document.getElementById('historySearch');
    M.Autocomplete.init(el, {
        data: {},
        onAutocomplete: function(val) {
            currentPage = 0;
            loadHistory();
        },
        minLength: 1
    });

    el.addEventListener('input', debounce(async (e) => {
        const query = e.target.value;
        
        // Automatyczne filtrowanie przy wpisywaniu (jeśli nie wybieramy z autocomplete)
        currentPage = 0;
        loadHistory();

        if (query.length < 1) return;

        try {
            const response = await fetch(`/api/v1/repair-clients/search?query=${encodeURIComponent(query)}`);
            const data = await response.json();
            const autocompleteData = {};
            selectedClientsMap = {};

            data.content.forEach(client => {
                const display = `${client.name} ${client.surname} (${client.phoneNumber})`;
                autocompleteData[display] = null;
                selectedClientsMap[display] = client;
            });

            const instance = M.Autocomplete.getInstance(el);
            instance.updateData(autocompleteData);
            instance.open();
        } catch (error) {
            console.error('Error fetching clients:', error);
        }
    }, 300));
}

function initDatePickers() {
    const commonOptions = {
        mode: "range",
        dateFormat: "Y-m-d",
        locale: flatpickr.l10ns.pl,
        monthSelectorType: "dropdown",
        onClose: function(selectedDates) {
            if (selectedDates.length === 2 || selectedDates.length === 0) {
                currentPage = 0;
                loadHistory();
            }
        }
    };

    receiptPicker = flatpickr("#receiptDateRange", commonOptions);
    handoverPicker = flatpickr("#handoverDateRange", commonOptions);
}

async function loadHistory() {
    const query = document.getElementById('historySearch').value;
    
    let url = `/api/v1/repairs/history?page=${currentPage}&size=${pageSize}&sort=handoverDate,DESC`;

    if (query) url += `&query=${encodeURIComponent(query)}`;
    
    if (receiptPicker && receiptPicker.selectedDates.length === 2) {
        const from = formatDate(receiptPicker.selectedDates[0]);
        const to = formatDate(receiptPicker.selectedDates[1]);
        url += `&receiptDateFrom=${encodeURIComponent(from + 'T00:00:00')}`;
        url += `&receiptDateTo=${encodeURIComponent(to + 'T23:59:59')}`;
    }

    if (handoverPicker && handoverPicker.selectedDates.length === 2) {
        const from = formatDate(handoverPicker.selectedDates[0]);
        const to = formatDate(handoverPicker.selectedDates[1]);
        url += `&handoverDateFrom=${encodeURIComponent(from + 'T00:00:00')}`;
        url += `&handoverDateTo=${encodeURIComponent(to + 'T23:59:59')}`;
    }

    try {
        const response = await fetch(url);
        const data = await response.json();
        renderHistory(data.content);
        renderPagination(data);
    } catch (error) {
        console.error('Error loading history:', error);
        M.toast({html: 'Błąd ładowania historii', classes: 'red'});
    }
}

function formatDate(date) {
    const d = new Date(date);
    let month = '' + (d.getMonth() + 1);
    let day = '' + d.getDate();
    const year = d.getFullYear();

    if (month.length < 2) month = '0' + month;
    if (day.length < 2) day = '0' + day;

    return [year, month, day].join('-');
}

function renderHistory(repairs) {
    const container = document.getElementById('historyCollapsible');
    container.innerHTML = '';

    if (repairs.length === 0) {
        container.innerHTML = '<li class="center-align grey-text p-10">Brak napraw spełniających kryteria</li>';
        return;
    }

    repairs.forEach(repair => {
        const li = document.createElement('li');
        const statusColor = getStatusColor(repair.status);
        const handoverDateStr = repair.handoverDate ? new Date(repair.handoverDate).toLocaleDateString() : '---';
        const receiptDateStr = repair.receiptDate ? new Date(repair.receiptDate).toLocaleDateString() : '---';

        const isArchived = repair.status === 'ARCHIWALNA';
        const goToBoardBtn = !isArchived ? `
            <button class="btn-small orange darken-2 waves-effect waves-light" onclick="goToBoard('${repair.technicalId}')" style="margin-right: 10px;">
                <i class="material-icons left">dashboard</i>Do tablicy
            </button>
        ` : '';

        li.innerHTML = `
            <div class="collapsible-header">
                <i class="material-icons ${statusColor}-text">history</i>
                <span style="flex: 1">
                    <b>${repair.manufacturer || ''} ${repair.model}</b> - ${repair.clientName} ${repair.clientSurname}
                    ${repair.businessId ? `<span class="blue-text" style="margin-left: 10px;">[${repair.businessId}]</span>` : ''}
                </span>
                <span class="grey-text hide-on-small-only" style="margin-right: 20px;">
                    Przyjęto: ${receiptDateStr} | Oddano: ${handoverDateStr}
                </span>
                <span class="badge ${statusColor} white-text" style="border-radius: 4px; min-width: 100px;">${repair.status}</span>
            </div>
            <div class="collapsible-body">
                <div class="row">
                    <div class="col s12 m6">
                        <p><b>RMA:</b> ${repair.businessId || '-'}</p>
                        <p><b>IMEI:</b> ${repair.imei || 'Brak'}</p>
                        <p><b>Klient:</b> ${repair.clientName} ${repair.clientSurname} (${repair.clientPhoneNumber || 'Brak telefonu'})</p>
                        <p><b>Opis problemu:</b> ${repair.problemDescription || repair.damageDescription || 'Brak'}</p>
                        <p><b>Uwagi:</b> ${repair.remarks || repair.repairOrderDescription || 'Brak'}</p>
                    </div>
                    <div class="col s12 m6">
                        <p><b>Koszt szacowany:</b> ${repair.estimatedCost ? repair.estimatedCost + ' zł' : '---'}</p>
                        <p><b>Cena ostateczna:</b> ${repair.repairPrice ? repair.repairPrice + ' zł' : '---'}</p>
                        <p><b>Typ:</b> ${repair.forCustomer ? 'Dla klienta' : 'Na sklep'}</p>
                    </div>
                </div>
                <div class="right-align">
                    ${goToBoardBtn}
                    <button class="btn-small blue darken-2 waves-effect waves-light" onclick="downloadPdf('${repair.technicalId}')">
                        <i class="material-icons left">description</i>Pobierz dokument
                    </button>
                </div>
            </div>
        `;
        container.appendChild(li);
    });
    M.Collapsible.init(container);
}

function goToBoard(technicalId) {
    window.location.href = `repair.html?highlight=${technicalId}`;
}

function getStatusColor(status) {
    switch (status) {
        case 'DO_NAPRAWY': return 'blue';
        case 'W_NAPRAWIE': return 'orange';
        case 'NAPRAWIONY': return 'green';
        case 'NIE_DO_NAPRAWY': return 'red';
        case 'ANULOWANY': return 'grey';
        case 'ARCHIWALNA': return 'blue-grey';
        default: return 'blue';
    }
}

function renderPagination(data) {
    const container = document.getElementById('paginationContainer');
    container.innerHTML = '';

    if (data.totalPages <= 1) return;

    const ul = document.createElement('ul');
    ul.className = 'pagination';

    // Back
    ul.innerHTML += `
        <li class="${data.first ? 'disabled' : 'waves-effect'}">
            <a href="#!" onclick="${data.first ? '' : 'changePage(' + (currentPage - 1) + ')'}">
                <i class="material-icons">chevron_left</i>
            </a>
        </li>
    `;

    // Pages
    for (let i = 0; i < data.totalPages; i++) {
        ul.innerHTML += `
            <li class="${i === currentPage ? 'active blue darken-2' : 'waves-effect'}">
                <a href="#!" onclick="changePage(${i})">${i + 1}</a>
            </li>
        `;
    }

    // Next
    ul.innerHTML += `
        <li class="${data.last ? 'disabled' : 'waves-effect'}">
            <a href="#!" onclick="${data.last ? '' : 'changePage(' + (currentPage + 1) + ')'}">
                <i class="material-icons">chevron_right</i>
            </a>
        </li>
    `;

    container.appendChild(ul);
}

function changePage(page) {
    currentPage = page;
    loadHistory();
}

function downloadPdf(technicalId) {
    window.open(`/api/v1/repairs/${technicalId}/handover`, '_blank');
}
