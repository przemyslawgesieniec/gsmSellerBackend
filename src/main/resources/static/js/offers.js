let photos = [];
let phoneSelect;
let currentPage = 0;

document.addEventListener('DOMContentLoaded', function() {
    initPhoneSelect();
    loadOffers();

    document.getElementById('photoFiles').addEventListener('change', handleFileSelect);
    document.getElementById('offerForm').addEventListener('submit', saveOffer);
    document.getElementById('clearPhone').addEventListener('click', clearSelectedPhone);
    document.getElementById('cancelBtn').addEventListener('click', resetForm);
});

function initPhoneSelect() {
    phoneSelect = new TomSelect('#phoneSelect', {
        valueField: 'technicalId',
        labelField: 'displayText',
        searchField: ['name', 'model', 'imei'],
        load: function(query, callback) {
            const url = `/api/v1/offers/available-phones?search=${encodeURIComponent(query)}&size=10`;
            fetch(url)
                .then(response => response.json())
                .then(json => {
                    callback(json.content.map(p => ({
                        ...p,
                        displayText: `${p.name} ${p.model} (IMEI: ${p.imei})`
                    })));
                }).catch(() => {
                    callback();
                });
        },
        onChange: function(value) {
            if (value) {
                const data = phoneSelect.options[value];
                selectPhone(data);
            }
        },
        render: {
            option: function(item, escape) {
                return `<div>
                    <span class="title"><b>${escape(item.name)}</b> ${escape(item.model)}</span><br>
                    <small class="grey-text">IMEI: ${escape(item.imei)} | Cena: ${escape(item.sellingPrice)} PLN</small>
                </div>`;
            }
        }
    });
}

function selectPhone(phone) {
    document.getElementById('phoneSelectWrapper').classList.add('hide');
    document.getElementById('selectedPhoneInfo').classList.remove('hide');
    document.getElementById('phoneDetails').textContent = `${phone.name} ${phone.model} (IMEI: ${phone.imei})`;
    document.getElementById('editTechnicalId').value = phone.technicalId;
    document.getElementById('brand').value = phone.name;
    M.updateTextFields();
    fetchAiSpecs(phone.name, phone.model);
}

async function fetchAiSpecs(name, model) {
    M.toast({html: 'Pobieranie specyfikacji z AI...', classes: 'blue'});
    try {
        const response = await fetch(`/api/v1/offers/ai-specs?name=${encodeURIComponent(name)}&model=${encodeURIComponent(model)}`);
        if (response.ok) {
            const specs = await response.json();
            fillFormWithAiSpecs(specs);
            M.toast({html: 'Specyfikacja uzupełniona przez AI', classes: 'green'});
        } else {
            M.toast({html: 'AI nie mogło znaleźć specyfikacji', classes: 'orange'});
        }
    } catch (err) {
        console.error('Błąd AI:', err);
        M.toast({html: 'Błąd połączenia z AI', classes: 'red'});
    }
}

function fillFormWithAiSpecs(specs) {
    if (specs.brand) document.getElementById('brand').value = specs.brand;
    if (specs.screen) {
        document.getElementById('screenSize').value = specs.screen.size || '';
        document.getElementById('screenResolution').value = specs.screen.resolution || '';
        document.getElementById('screenType').value = specs.screen.type || '';
    }
    document.getElementById('memory').value = specs.memory || '';
    document.getElementById('ram').value = specs.ram || '';
    document.getElementById('simCardType').value = specs.simCardType || '';
    document.getElementById('frontCamerasMpx').value = (specs.frontCamerasMpx || []).join(', ');
    document.getElementById('backCamerasMpx').value = (specs.backCamerasMpx || []).join(', ');
    document.getElementById('batteryCapacity').value = specs.batteryCapacity || '';
    document.getElementById('operatingSystem').value = specs.operatingSystem || '';
    if (specs.communication) {
        document.getElementById('wifi').value = specs.communication.wifi || '';
        document.getElementById('portType').value = specs.communication.portType || '';
        document.getElementById('bluetoothVersion').value = specs.communication.bluetoothVersion || '';
    }
    M.updateTextFields();
}

function clearSelectedPhone() {
    document.getElementById('phoneSelectWrapper').classList.remove('hide');
    document.getElementById('selectedPhoneInfo').classList.add('hide');
    document.getElementById('editTechnicalId').value = '';
    phoneSelect.clear();
    phoneSelect.clearOptions();
}

function handleFileSelect(e) {
    const files = Array.from(e.target.files);
    if (photos.filter(p => p instanceof File).length + files.length > 5) {
        M.toast({html: 'Można dodać maksymalnie 5 zdjęć', classes: 'red'});
        // reset inputu file
        e.target.value = '';
        return;
    }
    
    files.forEach(file => {
        photos.push(file);
    });
    
    renderPhotosPreview();
    // resetujemy input, by móc dodać te same pliki jeśli ktoś je usunie i będzie chciał dodać znowu
    e.target.value = '';
}

function renderPhotosPreview() {
    const container = document.getElementById('photosPreview');
    container.innerHTML = '';
    
    photos.forEach((photo, index) => {
        const div = document.createElement('div');
        div.className = 'photo-container';
        
        let src;
        if (photo instanceof File) {
            src = URL.createObjectURL(photo);
        } else {
            src = `/api/v1/external/offers/photos/${photo}/thumbnail`;
        }
        
        div.innerHTML = `
            <img src="${src}" alt="Foto ${index+1}">
            <i class="material-icons remove-photo" onclick="removePhoto(${index})">close</i>
        `;
        container.appendChild(div);
    });
}

function removePhoto(index) {
    const photo = photos[index];
    if (photo instanceof File) {
        URL.revokeObjectURL(photo);
    }
    photos.splice(index, 1);
    renderPhotosPreview();
}

async function saveOffer(e) {
    e.preventDefault();
    
    const technicalId = document.getElementById('editTechnicalId').value;
    if (!technicalId) {
        M.toast({html: 'Wybierz telefon!', classes: 'red'});
        return;
    }
    
    const isEdit = document.getElementById('cancelBtn').classList.contains('hide') === false;
    
    const formData = new FormData();
    if (!isEdit) {
        formData.append('phoneStockTechnicalId', technicalId);
    }
    formData.append('brand', document.getElementById('brand').value);
    formData.append('screenSize', document.getElementById('screenSize').value);
    formData.append('screenResolution', document.getElementById('screenResolution').value);
    formData.append('screenType', document.getElementById('screenType').value);
    formData.append('memory', document.getElementById('memory').value);
    formData.append('ram', document.getElementById('ram').value);
    formData.append('simCardType', document.getElementById('simCardType').value);
    formData.append('frontCamerasMpx', document.getElementById('frontCamerasMpx').value);
    formData.append('backCamerasMpx', document.getElementById('backCamerasMpx').value);
    formData.append('batteryCapacity', document.getElementById('batteryCapacity').value);
    formData.append('operatingSystem', document.getElementById('operatingSystem').value);
    formData.append('wifi', document.getElementById('wifi').value);
    formData.append('portType', document.getElementById('portType').value);
    formData.append('bluetoothVersion', document.getElementById('bluetoothVersion').value);
    
    photos.forEach(photo => {
        if (photo instanceof File) {
            formData.append('photoFiles', photo);
        } else if (typeof photo === 'string') {
            formData.append('photos', photo);
        }
    });
    
    const url = isEdit ? `/api/v1/offers/${technicalId}` : '/api/v1/offers';
    const method = isEdit ? 'PUT' : 'POST';
    
    try {
        const response = await fetch(url, {
            method: method,
            body: formData
        });
        
        if (response.ok) {
            M.toast({html: isEdit ? 'Oferta zaktualizowana!' : 'Oferta utworzona!', classes: 'green'});
            resetForm();
            loadOffers(isEdit ? currentPage : 0);
        } else {
            const err = await response.json();
            M.toast({html: 'Błąd: ' + (err.message || 'Nieznany błąd'), classes: 'red'});
        }
    } catch (err) {
        M.toast({html: 'Błąd połączenia', classes: 'red'});
    }
}

function resetForm() {
    document.getElementById('offerForm').reset();
    document.getElementById('editTechnicalId').value = '';
    photos = [];
    renderPhotosPreview();
    clearSelectedPhone();
    
    document.getElementById('formTitle').textContent = 'Stwórz nową ofertę';
    document.getElementById('cancelBtn').classList.add('hide');
    document.getElementById('phoneSelectWrapper').classList.remove('hide');
}

async function loadOffers(page = 0) {
    currentPage = page;
    document.getElementById('offersLoader').classList.remove('hide');
    try {
        const response = await fetch(`/api/v1/external/offers?page=${page}&size=12`);
        const data = await response.json();
        
        // Jeśli aktualna strona stała się pusta (np. po usunięciu), wróć do poprzedniej (o ile to nie strona 0)
        if (data.content.length === 0 && page > 0) {
            loadOffers(page - 1);
            return;
        }

        renderOffers(data.content);
        renderPagination(data);
    } catch (err) {
        M.toast({html: 'Błąd ładowania ofert', classes: 'red'});
    } finally {
        document.getElementById('offersLoader').classList.add('hide');
    }
}

function renderOffers(offers) {
    const container = document.getElementById('offers-list');
    container.innerHTML = '';
    
    if (offers.length === 0) {
        container.innerHTML = '<div class="col s12 center-align grey-text">Brak aktywnych ofert.</div>';
        return;
    }
    
    offers.forEach(offer => {
        const mainPhoto = offer.photos && offer.photos.length > 0 ? `/api/v1/external/offers/photos/${offer.photos[0]}/thumbnail` : 'https://via.placeholder.com/300x200?text=Brak+zdjęcia';
        
        const col = document.createElement('div');
        col.className = 'col s12 m6 l4';
        col.innerHTML = `
            <div class="card sticky-action">
                <div class="card-image waves-effect waves-block waves-light">
                    <img class="activator" src="${mainPhoto}" style="height: 200px; object-fit: cover;">
                </div>
                <div class="card-content">
                    <span class="card-title activator grey-text text-darken-4">
                        ${offer.brand} ${offer.model}
                        <i class="material-icons right">more_vert</i>
                    </span>
                    <p class="blue-text text-darken-2" style="font-weight: bold; font-size: 1.2rem;">
                        ${offer.price} PLN
                    </p>
                </div>
                <div class="card-action">
                    <a href="#" onclick="editOffer('${offer.technicalId}')" class="blue-text">Edytuj</a>
                    <a href="#" onclick="deleteOffer('${offer.technicalId}')" class="red-text right">Usuń</a>
                </div>
                <div class="card-reveal">
                    <span class="card-title grey-text text-darken-4">${offer.brand} ${offer.model}<i class="material-icons right">close</i></span>
                    <p>
                        <b>Ekran:</b> ${(offer.screen && offer.screen.size) || '---'} (${(offer.screen && offer.screen.resolution) || '---'})<br>
                        <b>Typ:</b> ${(offer.screen && offer.screen.type) || '---'}<br>
                        <b>Bateria:</b> ${offer.batteryCapacity || '---'}<br>
                        <b>Pamięć:</b> ${offer.memory || '---'} / ${offer.ram || '---'}<br>
                        <b>System:</b> ${offer.operatingSystem || '---'}
                    </p>
                    <div class="offer-photos-mini">
                        ${(offer.photos || []).map(p => `<img src="/api/v1/external/offers/photos/${p}/thumbnail" onclick="window.open('/api/v1/external/offers/photos/${p}', '_blank')" style="width: 50px; height: 50px; cursor: pointer; object-fit: cover; margin-right: 5px; border-radius: 2px;">`).join('')}
                    </div>
                </div>
            </div>
        `;
        container.appendChild(col);
    });
}

async function editOffer(technicalId) {
    try {
        const response = await fetch(`/api/v1/external/offers/${technicalId}`);
        const offer = await response.json();
        
        document.getElementById('formTitle').textContent = 'Edytuj ofertę';
        document.getElementById('cancelBtn').classList.remove('hide');
        document.getElementById('editTechnicalId').value = offer.technicalId;
        
        document.getElementById('brand').value = offer.brand || '';
        document.getElementById('screenSize').value = (offer.screen && offer.screen.size) || '';
        document.getElementById('screenResolution').value = (offer.screen && offer.screen.resolution) || '';
        document.getElementById('screenType').value = (offer.screen && offer.screen.type) || '';
        document.getElementById('memory').value = offer.memory || '';
        document.getElementById('ram').value = offer.ram || '';
        document.getElementById('simCardType').value = offer.simCardType || '';
        document.getElementById('frontCamerasMpx').value = (offer.frontCamerasMpx || []).join(', ');
        document.getElementById('backCamerasMpx').value = (offer.backCamerasMpx || []).join(', ');
        document.getElementById('batteryCapacity').value = offer.batteryCapacity || '';
        document.getElementById('operatingSystem').value = offer.operatingSystem || '';
        document.getElementById('wifi').value = (offer.communication && offer.communication.wifi) || '';
        document.getElementById('portType').value = (offer.communication && offer.communication.portType) || '';
        document.getElementById('bluetoothVersion').value = (offer.communication && offer.communication.bluetoothVersion) || '';
        
        photos = offer.photos || [];
        renderPhotosPreview();
        
        // Pokazujemy info o telefonie zamiast selecta
        document.getElementById('phoneSelectWrapper').classList.add('hide');
        document.getElementById('selectedPhoneInfo').classList.remove('hide');
        document.getElementById('phoneDetails').textContent = `${offer.brand} ${offer.model}`;
        
        M.updateTextFields();
        window.scrollTo({ top: 0, behavior: 'smooth' });
    } catch (err) {
        M.toast({html: 'Błąd pobierania danych oferty', classes: 'red'});
    }
}

async function deleteOffer(technicalId) {
    if (confirm('Czy na pewno chcesz usunąć tę ofertę? Zdjęcia zostaną również usunięte.')) {
        try {
            const response = await fetch(`/api/v1/offers/${technicalId}`, {
                method: 'DELETE'
            });

            if (response.ok) {
                M.toast({html: 'Oferta została usunięta', classes: 'green'});
                loadOffers(currentPage);
            } else {
                const err = await response.json();
                M.toast({html: 'Błąd podczas usuwania: ' + (err.message || 'Nieznany błąd'), classes: 'red'});
            }
        } catch (err) {
            M.toast({html: 'Błąd połączenia', classes: 'red'});
        }
    }
}

function renderPagination(data) {
    const pagination = document.getElementById("offersPagination");
    if (!pagination) return;
    pagination.innerHTML = "";

    function createPageItem({classes = "", inner = "", onClick = null}) {
        const li = document.createElement("li");
        if (classes) li.className = classes;

        const a = document.createElement("a");
        a.href = "#!";
        if (typeof inner === "string") a.innerHTML = inner;
        else if (inner instanceof Node) a.appendChild(inner);

        if (typeof onClick === "function") {
            a.addEventListener("click", (e) => {
                e.preventDefault();
                onClick();
            });
        } else {
            a.setAttribute("aria-disabled", "true");
            a.style.pointerEvents = "none";
        }

        li.appendChild(a);
        return li;
    }

    const { totalPages, number: currentPage } = data;

    if (totalPages <= 1) return;

    // ========== PRZYCISK "POPRZEDNIA" ==========
    if (currentPage > 0) {
        const leftIcon = '<i class="material-icons">chevron_left</i>';
        pagination.appendChild(createPageItem({
            classes: "waves-effect",
            inner: leftIcon,
            onClick: () => loadOffers(currentPage - 1)
        }));
    } else {
        const leftIcon = '<i class="material-icons">chevron_left</i>';
        pagination.appendChild(createPageItem({
            classes: "disabled",
            inner: leftIcon,
            onClick: null
        }));
    }

    // ========== LOGIKA WYŚWIETLANIA MAKS 7 STRON ==========
    const maxVisible = 7;
    let startPage = Math.max(0, currentPage - Math.floor(maxVisible / 2));
    let endPage = startPage + maxVisible - 1;

    if (endPage >= totalPages) {
        endPage = totalPages - 1;
        startPage = Math.max(0, endPage - maxVisible + 1);
    }

    for (let i = startPage; i <= endPage; i++) {
        if (i === currentPage) {
            pagination.appendChild(createPageItem({
                classes: "active blue",
                inner: String(i + 1),
                onClick: null
            }));
        } else {
            pagination.appendChild(createPageItem({
                classes: "waves-effect",
                inner: String(i + 1),
                onClick: () => loadOffers(i)
            }));
        }
    }

    // ========== PRZYCISK "NASTĘPNA" ==========
    if (currentPage < totalPages - 1) {
        const rightIcon = '<i class="material-icons">chevron_right</i>';
        pagination.appendChild(createPageItem({
            classes: "waves-effect",
            inner: rightIcon,
            onClick: () => loadOffers(currentPage + 1)
        }));
    } else {
        const rightIcon = '<i class="material-icons">chevron_right</i>';
        pagination.appendChild(createPageItem({
            classes: "disabled",
            inner: rightIcon,
            onClick: null
        }));
    }
}
