let photos = [];
let phoneSelect;

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
}

function clearSelectedPhone() {
    document.getElementById('phoneSelectWrapper').classList.remove('hide');
    document.getElementById('selectedPhoneInfo').classList.add('hide');
    document.getElementById('editTechnicalId').value = '';
    phoneSelect.clear();
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
            src = `/uploads/${photo}`;
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
    formData.append('screenSize', document.getElementById('screenSize').value);
    formData.append('batteryCapacity', document.getElementById('batteryCapacity').value);
    formData.append('screenType', document.getElementById('screenType').value);
    
    photos.forEach(photo => {
        if (photo instanceof File) {
            formData.append('photoFiles', photo);
        } else {
            formData.append('existingPhotos', photo);
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
            loadOffers();
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
    document.getElementById('offersLoader').classList.remove('hide');
    try {
        const response = await fetch(`/api/v1/offers?page=${page}&size=12`);
        const data = await response.json();
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
        const mainPhoto = offer.photos && offer.photos.length > 0 ? `/uploads/${offer.photos[0]}` : 'https://via.placeholder.com/300x200?text=Brak+zdjęcia';
        
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
                </div>
                <div class="card-reveal">
                    <span class="card-title grey-text text-darken-4">${offer.brand} ${offer.model}<i class="material-icons right">close</i></span>
                    <p>
                        <b>Ekran:</b> ${offer.screenSize || '---'}<br>
                        <b>Typ:</b> ${offer.screenType || '---'}<br>
                        <b>Bateria:</b> ${offer.batteryCapacity || '---'}
                    </p>
                    <div class="offer-photos-mini">
                        ${(offer.photos || []).map(p => `<img src="/uploads/${p}" style="width: 50px; height: 50px; object-fit: cover; margin-right: 5px; border-radius: 2px;">`).join('')}
                    </div>
                </div>
            </div>
        `;
        container.appendChild(col);
    });
}

async function editOffer(technicalId) {
    try {
        const response = await fetch(`/api/v1/offers/${technicalId}`);
        const offer = await response.json();
        
        document.getElementById('formTitle').textContent = 'Edytuj ofertę';
        document.getElementById('cancelBtn').classList.remove('hide');
        document.getElementById('editTechnicalId').value = offer.technicalId;
        
        document.getElementById('screenSize').value = offer.screenSize || '';
        document.getElementById('batteryCapacity').value = offer.batteryCapacity || '';
        document.getElementById('screenType').value = offer.screenType || '';
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

function renderPagination(data) {
    const container = document.getElementById('offersPagination');
    container.innerHTML = '';
    
    if (data.totalPages <= 1) return;
    
    for (let i = 0; i < data.totalPages; i++) {
        const li = document.createElement('li');
        li.className = i === data.number ? 'active blue' : 'waves-effect';
        li.innerHTML = `<a href="#!">${i + 1}</a>`;
        li.onclick = () => loadOffers(i);
        container.appendChild(li);
    }
}
