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
        document.getElementById('repairPurchasePriceWrapper').classList.add('hide');
        M.updateTextFields();
        M.Modal.getInstance(document.getElementById('repairModal')).open();
    });
});

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

    div.innerHTML = `
        <div class="card-content">
            ${statusLabel}
            <div style="clear: both;"></div>
            <span class="card-title">${repair.name}</span>
            <p><b>IMEI:</b> ${repair.imei || '-'}</p>
            <p><b>Kolor:</b> ${repair.color || '-'}</p>
            <div class="divider" style="margin: 5px 0;"></div>
            ${repair.forCustomer ? '' : `<p><b>Cena zak.:</b> ${repair.purchasePrice ? repair.purchasePrice + ' zł' : '-'}</p>`}
            <p><b>Cena napr.:</b> ${repair.repairPrice ? repair.repairPrice + ' zł' : '-'}</p>
            
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

    div.innerHTML = `
        <div class="card z-depth-1 status-${repair.status}" onclick="openRepairDetails('${repair.technicalId}')">
            <div class="card-content">
                ${statusLabel}
                <div style="clear: both;"></div>
                <span class="card-title">${repair.name}</span>
                <div class="row" style="margin-bottom: 0;">
                    <div class="col s6">
                        <p><b>IMEI:</b> ${repair.imei || '-'}</p>
                        <p><b>Kolor:</b> ${repair.color || '-'}</p>
                    </div>
                    <div class="col s6 right-align">
                        ${repair.forCustomer ? '' : `<p><b>Zakup:</b> ${repair.purchasePrice ? repair.purchasePrice + ' zł' : '-'}</p>`}
                        <p><b>Naprawa:</b> ${repair.repairPrice ? repair.repairPrice + ' zł' : '-'}</p>
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
    document.getElementById('repairName').value = repair.name;
    document.getElementById('repairImei').value = repair.imei || '';
    document.getElementById('repairColor').value = repair.color || '';
    document.getElementById('repairPinPassword').value = repair.pinPassword || '';
    document.getElementById('repairPurchasePrice').value = repair.purchasePrice || '';
    document.getElementById('repairPrice').value = repair.repairPrice || '';
    document.getElementById('repairDamageDescription').value = repair.damageDescription || '';
    document.getElementById('repairOrderDescription').value = repair.repairOrderDescription || '';

    if (repair.forCustomer) {
        document.getElementById('repairPurchasePriceWrapper').classList.add('hide');
    } else {
        document.getElementById('repairPurchasePriceWrapper').classList.remove('hide');
    }

    M.updateTextFields();
    M.textareaAutoResize(document.getElementById('repairDamageDescription'));
    M.textareaAutoResize(document.getElementById('repairOrderDescription'));
    
    const modal = M.Modal.getInstance(document.getElementById('repairModal'));
    modal.open();
}

async function saveRepair() {
    const id = document.getElementById('repairTechnicalId').value;
    const existingRepair = id ? repairsData.find(r => r.technicalId === id) : null;

    const repairDto = {
        name: document.getElementById('repairName').value,
        imei: document.getElementById('repairImei').value,
        color: document.getElementById('repairColor').value,
        pinPassword: document.getElementById('repairPinPassword').value,
        purchasePrice: document.getElementById('repairPurchasePrice').value || null,
        repairPrice: document.getElementById('repairPrice').value || null,
        damageDescription: document.getElementById('repairDamageDescription').value,
        repairOrderDescription: document.getElementById('repairOrderDescription').value,
        forCustomer: existingRepair ? existingRepair.forCustomer : true
    };

    if (!repairDto.name) {
        M.toast({html: 'Nazwa urządzenia jest wymagana', classes: 'orange'});
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
