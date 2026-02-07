document.addEventListener('DOMContentLoaded', function() {
    loadCurrentUser().then(user => {
        if (IS_ADMIN) {
            document.getElementById('repairLocationSection').style.display = 'block';
        }
        
        // Ustawienie domyślnej lokalizacji w filtrze
        if (user && user.location) {
            const filterSelect = document.getElementById('filterLocationSelect');
            // Musimy poczekać aż lokalizacje się załadują, albo ustawić to po loadLocations
            // Ale loadLocations jest asynchroniczne.
        }
        
        loadLocations().then(() => {
            const filterSelect = document.getElementById('filterLocationSelect');
            if (user && user.location && filterSelect) {
                filterSelect.value = user.location;
                M.FormSelect.init(filterSelect);
            }
            loadRepairs();
        });
    });

    document.getElementById('saveRepairBtn').addEventListener('click', saveRepair);
    
    const filterSelect = document.getElementById('filterLocationSelect');
    if (filterSelect) {
        filterSelect.addEventListener('change', loadRepairs);
    }

    document.getElementById('editRepairBtn').addEventListener('click', () => {
        toggleFields(false);
        document.getElementById('editRepairBtn').style.display = 'none';
        document.getElementById('saveRepairBtn').style.display = 'inline-block';
    });
    document.getElementById('confirmRestoreBtn').addEventListener('click', confirmRestore);
    document.getElementById('selectedClientChip').addEventListener('click', (e) => {
        if (e.target.id !== 'clearSelectedClient') {
            const clientDisplay = document.getElementById('selectedClientDisplay').innerText;
            // Spodziewany format: "Imię Nazwisko (Telefon)"
            // Zmieniamy format przekazywany w query na "[Imię Nazwisko (Telefon)]"
            const match = clientDisplay.match(/(.*) \((.*)\)/);
            if (match) {
                const query = clientDisplay;
                window.location.href = `serviceHistory.html?query=${encodeURIComponent(query)}`;
            }
        }
    });
    loadLocations();

    // Floating button reset form
    const fab = document.querySelector('.fab-fixed a');
    fab.addEventListener('click', () => {
        resetForm();
        toggleFields(false);
        document.getElementById('editRepairBtn').style.display = 'none';
        document.getElementById('saveRepairBtn').style.display = 'inline-block';
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
    
    // Inicjalizacja selectów Materialize
    M.FormSelect.init(document.querySelectorAll('select'));

    loadCurrentUser().then(() => {
        if (IS_ADMIN) {
            document.getElementById('repairLocationSection').style.display = 'block';
        }
    });
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

    document.getElementById('clientAnonymous').addEventListener('change', (e) => {
        const isAnonymous = e.target.checked;
        const searchSection = document.getElementById('clientSearchSection');
        const selectedInfo = document.getElementById('selectedClientInfo');
        const newClientFields = document.getElementById('newClientFields');
        const nameSurnameFields = document.getElementById('clientNameSurnameFields');

        if (isAnonymous) {
            searchSection.style.display = 'none';
            selectedInfo.style.display = 'none';
            newClientFields.style.display = 'block';
            nameSurnameFields.style.display = 'none';
            
            // Czyścimy imię i nazwisko w trybie anonimowym
            document.getElementById('clientName').value = '';
            document.getElementById('clientSurname').value = '';
        } else {
            newClientFields.style.display = 'none';
            searchSection.style.display = 'block';
            nameSurnameFields.style.display = 'block';
        }
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
    const filterSelect = document.getElementById('filterLocationSelect');
    const locationFilter = filterSelect ? filterSelect.value : '';
    const url = locationFilter ? `/api/v1/repairs?location=${encodeURIComponent(locationFilter)}` : '/api/v1/repairs';
    
    try {
        const response = await fetch(url);
        if (!response.ok) throw new Error('Błąd pobierania danych');
        repairsData = await response.json();
        renderBoard();
        checkHighlight();
    } catch (error) {
        console.error(error);
        M.toast({html: 'Nie udało się załadować napraw', classes: 'red'});
    }
}

function checkHighlight() {
    const urlParams = new URLSearchParams(window.location.search);
    const highlightId = urlParams.get('highlight');
    if (highlightId) {
        const repair = repairsData.find(r => r.technicalId === highlightId);
        if (repair) {
            // Przełącz na odpowiedni tab na mobilce jeśli trzeba
            if (window.innerWidth <= 992) {
                let tabId = '';
                if (['NAPRAWIONY', 'ANULOWANY', 'NIE_DO_NAPRAWY'].includes(repair.status)) {
                    tabId = 'tab-ZAKONCZONY';
                } else {
                    tabId = `tab-${repair.status}`;
                }
                const instance = M.Tabs.getInstance(document.querySelector('.tabs'));
                if (instance) instance.select(tabId);
            }

            // Znajdź kartę i dodaj klasę
            setTimeout(() => {
                const cards = document.querySelectorAll(`[data-id="${highlightId}"]`);
                cards.forEach(card => {
                    card.classList.add('highlight-card');
                    card.scrollIntoView({ behavior: 'smooth', block: 'center' });
                });
            }, 100);
        }
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
    
    // Sprawdzenie czy karta jest "przestarzała" (> 7 dni w statusie DO_NAPRAWY lub W_NAPRAWIE)
    let overdueClass = '';
    let overdueIcon = '';
    if (['DO_NAPRAWY', 'W_NAPRAWIE'].includes(repair.status) && repair.createDateTime) {
        const createDate = new Date(repair.createDateTime);
        const now = new Date();
        const diffTime = Math.abs(now - createDate);
        const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
        if (diffDays > 7) {
            overdueClass = 'overdue';
            overdueIcon = `
                <div class="overdue-container" style="position: absolute; top: 5px; right: 5px; display: flex; align-items: center; color: #f57f17;">
                    <span style="font-size: 0.8rem; font-weight: bold; margin-right: 2px;">${diffDays} dni</span>
                    <i class="material-icons" style="font-size: 24px;">warning</i>
                </div>`;
        }
    }

    div.className = `card kanban-card z-depth-1 status-${repair.status} ${overdueClass}`;
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
    const clientPhone = repair.clientPhoneNumber ? repair.clientPhoneNumber : '-';

    div.innerHTML = `
        <div class="card-content">
            ${overdueIcon}
            ${statusLabel}
            ${repair.location ? `
                <p class="location" style="position: absolute; top: 10px; right: 10px; margin: 0; display: flex; align-items: center;">
                    <i class="material-icons tiny blue-text">place</i>
                    <b>${repair.location}</b>
                </p>
            ` : ''}
            <div style="clear: both;"></div>
            <span class="card-title">
                ${(repair.manufacturer ? repair.manufacturer + ' ' : '') + (repair.model || '')}
                <br>
                <small style="font-weight: bold; color: #1565c0;"> ${repair.businessId || '-'}</small>
            </span>
            <p><b>Klient:</b> ${clientDisplay}</p>
            <p><b>Tel:</b> ${clientPhone}</p>
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
    
    // Inicjalizacja tooltipów po wyrenderowaniu
    setTimeout(() => {
        M.Tooltip.init(div.querySelectorAll('.tooltipped'));
    }, 0);

    return div;
}

function createMobileCard(repair) {
    const div = document.createElement('div');
    div.className = 'col s12';
    
    // Sprawdzenie czy karta jest "przestarzała"
    let overdueClass = '';
    let overdueIcon = '';
    if (['DO_NAPRAWY', 'W_NAPRAWIE'].includes(repair.status) && repair.createDateTime) {
        const createDate = new Date(repair.createDateTime);
        const now = new Date();
        const diffTime = Math.abs(now - createDate);
        const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
        if (diffDays > 7) {
            overdueClass = 'overdue';
            overdueIcon = `
                <div class="overdue-container" style="position: absolute; top: 5px; right: 5px; display: flex; align-items: center; color: #f57f17;">
                    <span style="font-size: 0.8rem; font-weight: bold; margin-right: 2px;">${diffDays} dni</span>
                    <i class="material-icons" style="font-size: 24px;">warning</i>
                </div>`;
        }
    }

    const statusLabels = {
        'NAPRAWIONY': '<span class="new badge green left" data-badge-caption="naprawiony" style="margin-left: 0; margin-bottom: 5px;"></span>',
        'ANULOWANY': '<span class="new badge grey left" data-badge-caption="anulowany" style="margin-left: 0; margin-bottom: 5px;"></span>',
        'NIE_DO_NAPRAWY': '<span class="new badge red left" data-badge-caption="nie do naprawy" style="margin-left: 0; margin-bottom: 5px;"></span>'
    };
    const statusLabel = statusLabels[repair.status] || '';
    const clientDisplay = repair.clientName ? `${repair.clientName} ${repair.clientSurname}` : 'Brak danych klienta';
    const clientPhone = repair.clientPhoneNumber ? repair.clientPhoneNumber : '-';

    div.innerHTML = `
        <div class="card z-depth-1 status-${repair.status} ${overdueClass}" onclick="openRepairDetails('${repair.technicalId}')">
            <div class="card-content">
                ${overdueIcon}
                ${statusLabel}
                ${repair.location ? `
                    <p class="location" style="position: absolute; top: 10px; right: 10px; margin: 0; display: flex; align-items: center;">
                        <i class="material-icons tiny blue-text">place</i>
                        <b>${repair.location}</b>
                    </p>
                ` : ''}
                <div style="clear: both;"></div>
                <span class="card-title">
                    ${(repair.manufacturer ? repair.manufacturer + ' ' : '') + (repair.model || '')}
                    <br>
                    <small style="font-weight: bold; color: #1565c0;"> ${repair.businessId || '-'}</small>
                </span>
                <p><b>Klient:</b> ${clientDisplay}</p>
                <p><b>Tel:</b> ${clientPhone}</p>
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

    // Inicjalizacja tooltipów po wyrenderowaniu
    setTimeout(() => {
        M.Tooltip.init(div.querySelectorAll('.tooltipped'));
    }, 0);

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
    
    const repair = repairsData.find(r => r.technicalId === id);
    if (!repair || repair.status === newStatus) return;

    if (newStatus === 'ZAKONCZONY') {
        document.getElementById('statusSelectionRepairId').value = id;
        M.Modal.getInstance(document.getElementById('statusSelectionModal')).open();
        return;
    }
    
    // Dla innych statusów wymagamy potwierdzenia
    const statusNames = {
        'DO_NAPRAWY': 'DO NAPRAWY',
        'W_NAPRAWIE': 'W NAPRAWIE'
    };
    
    document.getElementById('confirmStatusChangeMessage').innerText = `Czy na pewno chcesz zmienić status zlecenia na ${statusNames[newStatus]}?`;
    const confirmBtn = document.getElementById('confirmStatusChangeBtn');
    
    // Usuwamy stare listenery (klonowanie węzła to najprostszy sposób w czystym JS)
    const newConfirmBtn = confirmBtn.cloneNode(true);
    confirmBtn.parentNode.replaceChild(newConfirmBtn, confirmBtn);
    
    newConfirmBtn.addEventListener('click', async () => {
        M.Modal.getInstance(document.getElementById('confirmStatusChangeModal')).close();
        await updateRepairStatus(id, newStatus);
    });
    
    M.Modal.getInstance(document.getElementById('confirmStatusChangeModal')).open();
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
    
    if (repair.businessId) {
        document.getElementById('rmaDisplay').style.display = 'block';
        document.getElementById('repairRma').value = repair.businessId;
    } else {
        document.getElementById('rmaDisplay').style.display = 'none';
    }

    document.getElementById('clientAnonymous').checked = repair.anonymous || false;
    const nameSurnameFields = document.getElementById('clientNameSurnameFields');
    if (repair.anonymous) {
        document.getElementById('clientSearchSection').style.display = 'none';
        document.getElementById('newClientFields').style.display = 'block';
        nameSurnameFields.style.display = 'none';
        document.getElementById('clientPhone').value = repair.clientPhoneNumber || '';
    } else {
        nameSurnameFields.style.display = 'block';
    }

    if (repair.deviceType) {
        document.getElementById('repairDeviceType').value = repair.deviceType;
        M.FormSelect.init(document.getElementById('repairDeviceType'));
    }

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

    if (repair.location) {
        document.getElementById('repairLocation').value = repair.location;
        M.FormSelect.init(document.getElementById('repairLocation'));
    } else {
        document.getElementById('repairLocation').value = "";
        M.FormSelect.init(document.getElementById('repairLocation'));
    }

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
    document.getElementById('repairAdvancePayment').value = repair.advancePayment || '';
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
    
    toggleFields(true);
    document.getElementById('editRepairBtn').style.display = 'inline-block';
    document.getElementById('saveRepairBtn').style.display = 'none';

    const modal = M.Modal.getInstance(document.getElementById('repairModal'));
    modal.open();
}

async function saveRepair() {
    const id = document.getElementById('repairTechnicalId').value;
    const existingRepair = id ? repairsData.find(r => r.technicalId === id) : null;

    const receiptDate = document.getElementById('repairReceiptDate').value;
    const estimatedDate = document.getElementById('repairEstimatedRepairDate').value;

    const repairDto = {
        anonymous: document.getElementById('clientAnonymous').checked,
        clientTechnicalId: document.getElementById('selectedClientTechnicalId').value || null,
        clientName: document.getElementById('clientName').value,
        clientSurname: document.getElementById('clientSurname').value,
        clientPhoneNumber: document.getElementById('clientPhone').value,
        
        deviceType: document.getElementById('repairDeviceType').value,
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
        advancePayment: document.getElementById('repairAdvancePayment').value || null,
        
        repairPrice: document.getElementById('repairPrice').value || null,
        purchasePrice: document.getElementById('repairPurchasePrice').value || null,
        
        forCustomer: existingRepair ? existingRepair.forCustomer : (document.getElementById('isForCustomer').value === 'true'),
        phoneTechnicalId: document.getElementById('phoneTechnicalId').value || null,
        location: IS_ADMIN ? document.getElementById('repairLocation').value : (existingRepair ? existingRepair.location : null)
    };

    if (!repairDto.model) {
        M.toast({html: 'Model urządzenia jest wymagany', classes: 'orange'});
        return;
    }

    if (repairDto.anonymous && !repairDto.clientPhoneNumber) {
        M.toast({html: 'Numer telefonu jest wymagany dla klienta anonimowego', classes: 'orange'});
        return;
    }

    if (!repairDto.clientTechnicalId && !repairDto.clientName && repairDto.forCustomer && !repairDto.anonymous) {
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
        
        toggleFields(true);
        document.getElementById('editRepairBtn').style.display = 'inline-block';
        document.getElementById('saveRepairBtn').style.display = 'none';
        
        // Jeśli to było nowe zlecenie, to zamykamy modal lub odświeżamy dane. 
        // W opisie zadania jest "Po jego utworzeniu pola powinny być nieedytowalne". 
        // Więc zablokowaliśmy je powyżej.
        
        M.Modal.getInstance(document.getElementById('repairModal')).close();
        loadRepairs();
    } catch (error) {
        console.error(error);
        M.toast({html: 'Błąd podczas zapisu', classes: 'red'});
    }
}

function resetForm() {
    document.getElementById('repairForm').reset();
    document.getElementById('rmaDisplay').style.display = 'none';
    document.getElementById('repairRma').value = '';
    document.getElementById('repairTechnicalId').value = '';
    document.getElementById('selectedClientTechnicalId').value = '';
    document.getElementById('clientSearchSection').style.display = 'block';
    document.getElementById('newClientFields').style.display = 'none';
    document.getElementById('clientNameSurnameFields').style.display = 'block';
    document.getElementById('selectedClientInfo').style.display = 'none';
    document.getElementById('clientSearch').value = '';
    document.getElementById('repairFinalPriceWrapper').style.display = 'none';
    document.getElementById('shopRepairFields').style.display = 'none';
    document.getElementById('isForCustomer').value = 'true';
    document.getElementById('phoneTechnicalId').value = '';
    
    // Resetuj device type do domyślnego
    document.getElementById('repairDeviceType').value = 'TELEFON';
    M.FormSelect.init(document.getElementById('repairDeviceType'));

    // Resetuj anonimowego klienta
    document.getElementById('clientAnonymous').checked = false;
}

function toggleFields(disabled) {
    const form = document.getElementById('repairForm');
    const elements = form.querySelectorAll('input, textarea, select, button:not(.btn-flat)');
    elements.forEach(el => {
        // Nie blokujemy ukrytych pól technicznych
        if (el.type !== 'hidden') {
            el.disabled = disabled;
        }
    });
    
    // Specyficzna obsługa dla chipa usuwania klienta
    const clearClient = document.getElementById('clearSelectedClient');
    if (clearClient) {
        clearClient.style.pointerEvents = disabled ? 'none' : 'auto';
        clearClient.style.opacity = disabled ? '0.5' : '1';
    }
    
    // Przycisk "Dodaj nowego klienta"
    const addNewClient = document.getElementById('btnAddNewClient');
    if (addNewClient) {
        addNewClient.style.pointerEvents = disabled ? 'none' : 'auto';
        addNewClient.style.opacity = disabled ? '0.5' : '1';
    }
}

function downloadRepairPdf(technicalId, type) {
    window.open(`/api/v1/repairs/${technicalId}/${type}`, '_blank');
}

async function loadLocations() {
    try {
        const response = await fetch('/api/v1/locations');
        if (!response.ok) throw new Error('Błąd pobierania lokalizacji');
        const locations = await response.json();
        
        const restoreSelect = document.getElementById('restoreLocation');
        const repairSelect = document.getElementById('repairLocation');
        const filterSelect = document.getElementById('filterLocationSelect');

        locations.forEach(loc => {
            const option = document.createElement('option');
            option.value = loc.name; // For repairLocation we use name
            option.text = loc.name;
            repairSelect.appendChild(option);

            const filterOption = document.createElement('option');
            filterOption.value = loc.name;
            filterOption.text = loc.name;
            filterSelect.appendChild(filterOption);

            const restoreOption = document.createElement('option');
            restoreOption.value = loc.technicalId; // For restoreLocation we use technicalId
            restoreOption.text = loc.name;
            restoreSelect.appendChild(restoreOption);
        });
        M.FormSelect.init(restoreSelect);
        M.FormSelect.init(repairSelect);
        M.FormSelect.init(filterSelect);
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
