document.addEventListener('DOMContentLoaded', function() {
    loadRepairs();

    document.getElementById('saveRepairBtn').addEventListener('click', saveRepair);
    document.getElementById('confirmRestoreBtn').addEventListener('click', confirmRestore);
    loadLocations();

    // Floating button reset form
    const fab = document.querySelector('.fab-fixed a');
    fab.addEventListener('click', () => {
        resetForm();
        document.getElementById('modalTitle').innerText = 'Nowe zlecenie naprawy';
        // Set default dates
        const today = new Date().toISOString().split('T')[0];
        const nextWeek = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString().split('T')[0];
        document.getElementById('repairReceiptDate').value = today;
        document.getElementById('repairEstimatedRepairDate').value = nextWeek;
        
        M.updateTextFields();
        M.Modal.getInstance(document.getElementById('repairModal')).open();
    });

    initClientAutocomplete();
    initClientToggles();
});

function initClientAutocomplete() {
    const el = document.getElementById('clientSearch');
    M.Autocomplete.init(el, {
        data: {},
        onAutocomplete: function(val) {
            const client = selectedClientsMap[val];
            if (client) {
                selectClient(client);
            }
        },
        minLength: 1
    });

    el.addEventListener('input', debounce(async (e) => {
        const query = e.target.value;
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

let selectedClientsMap = {};

function selectClient(client) {
    document.getElementById('selectedClientTechnicalId').value = client.technicalId;
    document.getElementById('selectedClientDisplay').innerText = `${client.name} ${client.surname} (${client.phoneNumber})`;
    document.getElementById('clientSearchSection').style.display = 'none';
    document.getElementById('selectedClientInfo').style.display = 'block';
    document.getElementById('newClientFields').style.display = 'none';
}

function initClientToggles() {
    document.getElementById('btnAddNewClient').addEventListener('click', () => {
        document.getElementById('clientSearchSection').style.display = 'none';
        document.getElementById('newClientFields').style.display = 'block';
        document.getElementById('selectedClientInfo').style.display = 'none';
        document.getElementById('selectedClientTechnicalId').value = '';
    });

    document.getElementById('cancelNewClient').addEventListener('click', () => {
        document.getElementById('clientSearchSection').style.display = 'block';
        document.getElementById('newClientFields').style.display = 'none';
        document.getElementById('repairForm').querySelectorAll('#newClientFields input').forEach(i => i.value = '');
    });

    document.getElementById('clearSelectedClient').addEventListener('click', () => {
        document.getElementById('clientSearchSection').style.display = 'block';
        document.getElementById('selectedClientInfo').style.display = 'none';
        document.getElementById('selectedClientTechnicalId').value = '';
        document.getElementById('clientSearch').value = '';
        M.updateTextFields();
    });
}

function debounce(func, wait) {
    let timeout;
    return function() {
        const context = this, args = arguments;
        clearTimeout(timeout);
        timeout = setTimeout(() => func.apply(context, args), wait);
    };
}

let repairsData = [];

async function loadRepairs() {
    try {
        const response = await fetch('/api/v1/repairs');
        if (!response.ok) throw new Error('Błąd pobierania danych');
        repairsData = await response.json();
        renderBoard();
    } catch (error) {
        console.error(error);
        M.toast({html: 'Nie udało się załadować napraw', classes: 'red'});
    }
}

function renderBoard() {
    const statuses = ['DO_NAPRAWY', 'W_NAPRAWIE', 'ZAKONCZONY'];
    const finishedStatuses = ['NAPRAWIONY', 'ANULOWANY', 'NIE_DO_NAPRAWY'];
    
    statuses.forEach(status => {
        const desktopContainer = document.getElementById(`cards-${status}`);
        const mobileContainer = document.getElementById(`mobile-cards-${status}`);
        
        desktopContainer.innerHTML = '';
        mobileContainer.innerHTML = '';
        
        const filteredRepairs = repairsData.filter(r => {
            if (status === 'ZAKONCZONY') {
                return finishedStatuses.includes(r.status);
            }
            return r.status === status;
        });
        
        filteredRepairs.forEach(repair => {
            const card = createCard(repair);
            desktopContainer.appendChild(card.cloneNode(true));
            mobileContainer.appendChild(createMobileCard(repair));
        });
    });
}

function createCard(repair) {
    const div = document.createElement('div');
    div.className = `card kanban-card z-depth-1 status-${repair.status}`;
    div.draggable = true;
    div.setAttribute('data-id', repair.technicalId);
    div.setAttribute('ondragstart', 'drag(event)');
    div.setAttribute('onclick', `openRepairDetails('${repair.technicalId}')`);

    const statusLabels = {
        'NAPRAWIONY': '<span class="new badge green left" data-badge-caption="naprawiony" style="margin-left: 0; margin-bottom: 5px;"></span>',
        'ANULOWANY': '<span class="new badge grey left" data-badge-caption="anulowany" style="margin-left: 0; margin-bottom: 5px;"></span>',
        'NIE_DO_NAPRAWY': '<span class="new badge red left" data-badge-caption="nie do naprawy" style="margin-left: 0; margin-bottom: 5px;"></span>'
    };
    const statusLabel = statusLabels[repair.status] || '';
    const clientDisplay = repair.clientName ? `${repair.clientName} ${repair.clientSurname}` : 'Brak danych klienta';

    div.innerHTML = `
        <div class="card-content">
            ${statusLabel}
            <div style="clear: both;"></div>
            <span class="card-title">${(repair.manufacturer ? repair.manufacturer + ' ' : '') + (repair.model || '')}</span>
            <p><b>Klient:</b> ${clientDisplay}</p>
            <p><b>IMEI:</b> ${repair.imei || '-'}</p>
            <div class="divider" style="margin: 5px 0;"></div>
            <p><b>Szac. koszt:</b> ${repair.estimatedCost ? repair.estimatedCost + ' zł' : '-'}</p>
            ${repair.repairPrice ? `<p><b>Koszt końcowy:</b> ${repair.repairPrice} zł</p>` : ''}
            
            ${['DO_NAPRAWY'].includes(repair.status) && repair.forCustomer ? `
                <div class="right-align" style="margin-top: 10px;">
                    <button class="btn-small blue darken-2 waves-effect waves-light" 
                            onclick="event.stopPropagation(); downloadRepairPdf('${repair.technicalId}', 'receipt')">
                        <i class="material-icons left">description</i>Pokwitowanie
                    </button>
                </div>
            ` : ''}
            
            ${['NAPRAWIONY', 'ANULOWANY', 'NIE_DO_NAPRAWY'].includes(repair.status) ? `
                <div class="right-align" style="margin-top: 10px;">
                    ${repair.forCustomer ? `
                        <button class="btn-small green darken-2 waves-effect waves-light" 
                                onclick="event.stopPropagation(); downloadRepairPdf('${repair.technicalId}', 'handover')">
                            <i class="material-icons left">check_circle</i>Odbiór
                        </button>
                    ` : ''}
                    ${!repair.forCustomer && repair.status === 'NAPRAWIONY' ? `
                        <button class="btn-small orange darken-2 waves-effect waves-light" 
                                onclick="event.stopPropagation(); openRestoreModal('${repair.technicalId}')">
                            <i class="material-icons left">store</i>Przywróć na sklep
                        </button>
                    ` : ''}
                </div>
            ` : ''}
        </div>
    `;
    return div;
}

function createMobileCard(repair) {
    const div = document.createElement('div');
    div.className = 'col s12';
    const statusLabels = {
        'NAPRAWIONY': '<span class="new badge green left" data-badge-caption="naprawiony" style="margin-left: 0; margin-bottom: 5px;"></span>',
        'ANULOWANY': '<span class="new badge grey left" data-badge-caption="anulowany" style="margin-left: 0; margin-bottom: 5px;"></span>',
        'NIE_DO_NAPRAWY': '<span class="new badge red left" data-badge-caption="nie do naprawy" style="margin-left: 0; margin-bottom: 5px;"></span>'
    };
    const statusLabel = statusLabels[repair.status] || '';
    const clientDisplay = repair.clientName ? `${repair.clientName} ${repair.clientSurname}` : 'Brak danych klienta';

    div.innerHTML = `
        <div class="card z-depth-1 status-${repair.status}" onclick="openRepairDetails('${repair.technicalId}')">
            <div class="card-content">
                ${statusLabel}
                <div style="clear: both;"></div>
                <span class="card-title">${(repair.manufacturer ? repair.manufacturer + ' ' : '') + (repair.model || '')}</span>
                <p><b>Klient:</b> ${clientDisplay}</p>
                <div class="row" style="margin-bottom: 0;">
                    <div class="col s6">
                        <p><b>IMEI:</b> ${repair.imei || '-'}</p>
                    </div>
                    <div class="col s6 right-align">
                        <p><b>Szac. koszt:</b> ${repair.estimatedCost ? repair.estimatedCost + ' zł' : '-'}</p>
                        ${repair.repairPrice ? `<p><b>Końcowy:</b> ${repair.repairPrice} zł</p>` : ''}
                    </div>
                </div>
                
                ${['DO_NAPRAWY'].includes(repair.status) && repair.forCustomer ? `
                    <div class="right-align" style="margin-top: 10px;">
                        <button class="btn blue darken-2 waves-effect waves-light" 
                                onclick="event.stopPropagation(); downloadRepairPdf('${repair.technicalId}', 'receipt')">
                            <i class="material-icons left">description</i>Pokwitowanie dla klienta
                        </button>
                    </div>
                ` : ''}
                
                ${['NAPRAWIONY', 'ANULOWANY', 'NIE_DO_NAPRAWY'].includes(repair.status) ? `
                    <div class="right-align" style="margin-top: 10px;">
                        ${repair.forCustomer ? `
                            <button class="btn green darken-2 waves-effect waves-light" 
                                    onclick="event.stopPropagation(); downloadRepairPdf('${repair.technicalId}', 'handover')">
                                <i class="material-icons left">check_circle</i>Potwierdzenie odbioru
                            </button>
                        ` : ''}
                        ${!repair.forCustomer && repair.status === 'NAPRAWIONY' ? `
                            <button class="btn orange darken-2 waves-effect waves-light" 
                                    onclick="event.stopPropagation(); openRestoreModal('${repair.technicalId}')">
                                <i class="material-icons left">store</i>Przywróć na sklep
                            </button>
                        ` : ''}
                    </div>
                ` : ''}
            </div>
        </div>
    `;
    return div;
}

// Drag & Drop logic
window.allowDrop = function(ev) {
    ev.preventDefault();
    ev.currentTarget.classList.add('drag-over');
}

window.dragLeave = function(ev) {
    ev.currentTarget.classList.remove('drag-over');
}

window.drag = function(ev) {
    const id = ev.target.closest('.kanban-card').getAttribute('data-id');
    ev.dataTransfer.setData("text", id);
}

window.drop = async function(ev) {
    ev.preventDefault();
    const container = ev.currentTarget.closest('.kanban-column');
    container.classList.remove('drag-over');
    
    const id = ev.dataTransfer.getData("text");
    const newStatus = container.id.replace('col-', '');
    
    if (newStatus === 'ZAKONCZONY') {
        document.getElementById('statusSelectionRepairId').value = id;
        M.Modal.getInstance(document.getElementById('statusSelectionModal')).open();
        return;
    }
    
    await updateRepairStatus(id, newStatus);
}

async function updateRepairStatus(id, newStatus) {
    const repair = repairsData.find(r => r.technicalId === id);
    if (repair && repair.status !== newStatus) {
        try {
            const response = await fetch(`/api/v1/repairs/${id}/status?status=${newStatus}`, {
                method: 'PATCH'
            });
            if (!response.ok) throw new Error('Błąd zmiany statusu');
            
            M.toast({html: 'Status zaktualizowany', classes: 'green'});
            loadRepairs();
        } catch (error) {
            console.error(error);
            M.toast({html: 'Błąd podczas zmiany statusu', classes: 'red'});
        }
    }
}

window.finalizeStatus = async function(selectedStatus) {
    const id = document.getElementById('statusSelectionRepairId').value;
    M.Modal.getInstance(document.getElementById('statusSelectionModal')).close();
    await updateRepairStatus(id, selectedStatus);
}

window.cancelStatusSelection = function() {
    M.Modal.getInstance(document.getElementById('statusSelectionModal')).close();
    loadRepairs(); // Refresh to move the card back to its original position visually
}

async function openRepairDetails(technicalId) {
    const repair = repairsData.find(r => r.technicalId === technicalId);
    if (!repair) return;

    resetForm();
    document.getElementById('modalTitle').innerText = 'Szczegóły naprawy';
    document.getElementById('repairTechnicalId').value = repair.technicalId;
    document.getElementById('isForCustomer').value = repair.forCustomer;
    document.getElementById('phoneTechnicalId').value = repair.phoneTechnicalId || '';
    
    // Klient
    if (repair.clientTechnicalId) {
        selectClient({
            technicalId: repair.clientTechnicalId,
            name: repair.clientName,
            surname: repair.clientSurname,
            phoneNumber: repair.clientPhoneNumber
        });
    }

    // Urządzenie
    document.getElementById('repairManufacturer').value = repair.manufacturer || '';
    document.getElementById('repairModel').value = repair.model || '';
    document.getElementById('repairImei').value = repair.imei || '';

    // Naprawa
    document.getElementById('repairProblemDescription').value = repair.problemDescription || repair.damageDescription || '';
    document.getElementById('repairDeviceCondition').value = repair.deviceCondition || '';
    document.getElementById('repairRemarks').value = repair.remarks || repair.repairOrderDescription || '';
    document.getElementById('repairLockCode').value = repair.lockCode || repair.pinPassword || '';
    
    document.getElementById('repairMoistureTraces').checked = repair.moistureTraces;
    document.getElementById('repairWarrantyRepair').checked = repair.warrantyRepair;
    document.getElementById('repairTurnsOn').checked = repair.turnsOn;

    if (repair.receiptDate) {
        document.getElementById('repairReceiptDate').value = repair.receiptDate.split('T')[0];
    }
    if (repair.estimatedRepairDate) {
        document.getElementById('repairEstimatedRepairDate').value = repair.estimatedRepairDate.split('T')[0];
    }
    document.getElementById('repairEstimatedCost').value = repair.estimatedCost || '';
    document.getElementById('repairPrice').value = repair.repairPrice || '';
    document.getElementById('repairPurchasePrice').value = repair.purchasePrice || '';

    // Visibility of price fields
    if (repair.status === 'NAPRAWIONY' || repair.status === 'NIE_DO_NAPRAWY') {
        document.getElementById('repairFinalPriceWrapper').style.display = 'block';
    } else {
        document.getElementById('repairFinalPriceWrapper').style.display = 'none';
    }

    if (repair.forCustomer) {
        document.getElementById('shopRepairFields').style.display = 'none';
    } else {
        document.getElementById('shopRepairFields').style.display = 'block';
    }

    M.updateTextFields();
    M.textareaAutoResize(document.getElementById('repairProblemDescription'));
    M.textareaAutoResize(document.getElementById('repairDeviceCondition'));
    M.textareaAutoResize(document.getElementById('repairRemarks'));
    
    const modal = M.Modal.getInstance(document.getElementById('repairModal'));
    modal.open();
}

async function saveRepair() {
    const id = document.getElementById('repairTechnicalId').value;
    const existingRepair = id ? repairsData.find(r => r.technicalId === id) : null;

    const receiptDate = document.getElementById('repairReceiptDate').value;
    const estimatedDate = document.getElementById('repairEstimatedRepairDate').value;

    const repairDto = {
        clientTechnicalId: document.getElementById('selectedClientTechnicalId').value || null,
        clientName: document.getElementById('clientName').value,
        clientSurname: document.getElementById('clientSurname').value,
        clientPhoneNumber: document.getElementById('clientPhone').value,
        
        manufacturer: document.getElementById('repairManufacturer').value,
        model: document.getElementById('repairModel').value,
        imei: document.getElementById('repairImei').value,
        
        problemDescription: document.getElementById('repairProblemDescription').value,
        deviceCondition: document.getElementById('repairDeviceCondition').value,
        remarks: document.getElementById('repairRemarks').value,
        lockCode: document.getElementById('repairLockCode').value,
        
        moistureTraces: document.getElementById('repairMoistureTraces').checked,
        warrantyRepair: document.getElementById('repairWarrantyRepair').checked,
        turnsOn: document.getElementById('repairTurnsOn').checked,
        
        receiptDate: receiptDate ? receiptDate + 'T00:00:00' : null,
        estimatedRepairDate: estimatedDate ? estimatedDate + 'T00:00:00' : null,
        estimatedCost: document.getElementById('repairEstimatedCost').value || null,
        
        repairPrice: document.getElementById('repairPrice').value || null,
        purchasePrice: document.getElementById('repairPurchasePrice').value || null,
        
        forCustomer: existingRepair ? existingRepair.forCustomer : (document.getElementById('isForCustomer').value === 'true'),
        phoneTechnicalId: document.getElementById('phoneTechnicalId').value || null
    };

    if (!repairDto.model) {
        M.toast({html: 'Model urządzenia jest wymagany', classes: 'orange'});
        return;
    }

    if (!repairDto.clientTechnicalId && !repairDto.clientName && repairDto.forCustomer) {
        M.toast({html: 'Wybierz klienta lub podaj dane nowego klienta', classes: 'orange'});
        return;
    }

    try {
        const url = id ? `/api/v1/repairs/${id}` : '/api/v1/repairs';
        const method = id ? 'PUT' : 'POST';
        
        const response = await fetch(url, {
            method: method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(repairDto)
        });

        if (!response.ok) throw new Error('Błąd zapisu');

        M.toast({html: 'Zapisano pomyślnie', classes: 'green'});
        M.Modal.getInstance(document.getElementById('repairModal')).close();
        loadRepairs();
    } catch (error) {
        console.error(error);
        M.toast({html: 'Błąd podczas zapisu', classes: 'red'});
    }
}

function resetForm() {
    document.getElementById('repairForm').reset();
    document.getElementById('repairTechnicalId').value = '';
    document.getElementById('selectedClientTechnicalId').value = '';
    document.getElementById('clientSearchSection').style.display = 'block';
    document.getElementById('newClientFields').style.display = 'none';
    document.getElementById('selectedClientInfo').style.display = 'none';
    document.getElementById('clientSearch').value = '';
    document.getElementById('repairFinalPriceWrapper').style.display = 'none';
    document.getElementById('shopRepairFields').style.display = 'none';
    document.getElementById('isForCustomer').value = 'true';
    document.getElementById('phoneTechnicalId').value = '';
}

function downloadRepairPdf(technicalId, type) {
    window.open(`/api/v1/repairs/${technicalId}/${type}`, '_blank');
}

async function loadLocations() {
    try {
        const response = await fetch('/api/v1/locations');
        if (!response.ok) throw new Error('Błąd pobierania lokalizacji');
        const locations = await response.json();
        
        const select = document.getElementById('restoreLocation');
        locations.forEach(loc => {
            const option = document.createElement('option');
            option.value = loc.technicalId;
            option.text = loc.name;
            select.appendChild(option);
        });
        M.FormSelect.init(select);
    } catch (error) {
        console.error(error);
    }
}

window.openRestoreModal = function(technicalId) {
    const repair = repairsData.find(r => r.technicalId === technicalId);
    if (!repair) return;

    document.getElementById('restoreRepairId').value = technicalId;
    document.getElementById('restoreRepairPrice').value = repair.repairPrice || '';
    document.getElementById('restoreSellingPrice').value = repair.purchasePrice || ''; // TODO: check if we should prefill with phone's selling price
    
    // Actually, the requirement says: pre-filled with "selling price" from the moment of adding.
    // We need to fetch the associated phone to get its selling price if we want to be accurate, 
    // but the repair object currently only stores purchasePrice.
    // Let's fetch the phone if phoneTechn  icalId is present.
    
    if (repair.phoneTechnicalId) {
        fetch(`/api/v1/phones/${repair.phoneTechnicalId}`)
            .then(res => res.json())
            .then(phone => {
                document.getElementById('restoreSellingPrice').value = phone.sellingPrice;
                M.updateTextFields();
            })
            .catch(err => console.error('Błąd pobierania danych telefonu', err));
    }

    M.updateTextFields();
    M.Modal.getInstance(document.getElementById('restoreToShopModal')).open();
}

async function confirmRestore() {
    const technicalId = document.getElementById('restoreRepairId').value;
    const request = {
        repairPrice: document.getElementById('restoreRepairPrice').value,
        sellingPrice: document.getElementById('restoreSellingPrice').value,
        locationTechnicalId: document.getElementById('restoreLocation').value
    };

    if (!request.locationTechnicalId) {
        M.toast({html: 'Wybierz lokalizację', classes: 'orange'});
        return;
    }

    try {
        const response = await fetch(`/api/v1/repairs/${technicalId}/restore-to-shop`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(request)
        });

        if (!response.ok) throw new Error('Błąd przywracania');

        M.toast({html: 'Telefon przywrócony na sklep', classes: 'green'});
        M.Modal.getInstance(document.getElementById('restoreToShopModal')).close();
        loadRepairs();
    } catch (error) {
        console.error(error);
        M.toast({html: 'Błąd podczas przywracania', classes: 'red'});
    }
}
