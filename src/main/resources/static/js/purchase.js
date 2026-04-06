document.addEventListener('DOMContentLoaded', function() {
    loadPurchases();

    const closeForm = document.getElementById('close-purchase-form');
    if (closeForm) {
        closeForm.addEventListener('submit', function(e) {
            e.preventDefault();
            closePurchase();
        });
    }
});

let currentPurchases = [];

async function loadPurchases() {
    try {
        const response = await fetch('/api/v1/purchase');
        if (!response.ok) throw new Error('Błąd pobierania danych');
        
        currentPurchases = await response.ok ? await response.json() : [];
        renderPurchases(currentPurchases);
    } catch (error) {
        console.error('Error loading purchases:', error);
        M.toast({html: 'Błąd ładowania ofert skupu', classes: 'red'});
    }
}

function renderPurchases(purchases) {
    const list = document.getElementById('purchase-list');
    list.innerHTML = '';

    purchases.forEach(p => {
        const row = document.createElement('tr');
        const date = new Date(p.createdAt).toLocaleString();
        
        row.innerHTML = `
            <td>${date}</td>
            <td>${p.phoneModel}</td>
            <td>${p.phoneNumber}</td>
            <td>${p.description || ''}</td>
            <td><span class="badge ${p.status === 'OPEN' ? 'blue' : 'grey'} white-text">${p.status}</span></td>
            <td>
                <button class="btn-small waves-effect waves-light" onclick="showDetails('${p.technicalId}')">
                    <i class="material-icons">visibility</i>
                </button>
            </td>
        `;
        list.appendChild(row);
    });
}

function showDetails(technicalId) {
    const p = currentPurchases.find(item => item.technicalId === technicalId);
    if (!p) return;

    document.getElementById('detail-model').textContent = p.phoneModel;
    document.getElementById('detail-phone').textContent = p.phoneNumber;
    document.getElementById('detail-date').textContent = new Date(p.createdAt).toLocaleString();
    document.getElementById('detail-description').textContent = p.description || 'Brak opisu';

    // Photos
    const photosContainer = document.getElementById('detail-photos');
    photosContainer.innerHTML = '<h5>Zdjęcia</h5>';
    if (p.photoIds && p.photoIds.length > 0) {
        p.photoIds.forEach(id => {
            const img = document.createElement('img');
            img.src = `/api/v1/purchase/photos/${id}`;
            img.className = 'responsive-img materialboxed';
            img.style.maxHeight = '150px';
            img.style.margin = '5px';
            photosContainer.appendChild(img);
        });
        M.Materialbox.init(document.querySelectorAll('.materialboxed'));
    } else {
        photosContainer.innerHTML += '<p>Brak zdjęć</p>';
    }

    // Closure logic
    const closureSection = document.getElementById('closure-section');
    const closedInfoSection = document.getElementById('closed-info-section');
    const closeForm = document.getElementById('close-purchase-form');

    if (p.status === 'OPEN') {
        closureSection.style.display = 'block';
        closedInfoSection.style.display = 'none';
        document.getElementById('close-purchase-id').value = p.technicalId;
        closeForm.reset();
        M.updateTextFields();
    } else {
        closureSection.style.display = 'none';
        closedInfoSection.style.display = 'block';
        document.getElementById('info-closure-reason').textContent = p.closureReason;
        document.getElementById('info-contacted').textContent = p.contactedCustomer ? 'Tak' : 'Nie';
    }

    const modal = M.Modal.getInstance(document.getElementById('modal-details'));
    if (modal) {
        modal.open();
    } else {
        M.Modal.init(document.getElementById('modal-details')).open();
    }
}

async function closePurchase() {
    const id = document.getElementById('close-purchase-id').value;
    const reason = document.getElementById('closure-reason').value;
    const contacted = document.getElementById('contacted-customer').checked;

    try {
        const response = await fetch(`/api/v1/purchase/${id}/close`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                reason: reason,
                contactedCustomer: contacted
            })
        });

        if (response.ok) {
            M.toast({html: 'Oferta została zamknięta', classes: 'green'});
            const modal = M.Modal.getInstance(document.getElementById('modal-details'));
            if (modal) {
                modal.close();
            }
            loadPurchases();
        } else {
            throw new Error('Błąd przy zamykaniu');
        }
    } catch (error) {
        console.error('Error closing purchase:', error);
        M.toast({html: 'Wystąpił błąd przy zamykaniu oferty', classes: 'red'});
    }
}
