let editedReceipt = null;
let editInProgress = false;
const receiptId = new URLSearchParams(window.location.search).get('id');
const editVatOptions = { VAT_EXEMPT: 'ZW', VAT_0: '0%', VAT_5: '5%', VAT_8: '8%', VAT_23: '23%' };

function setEditStatus(message, error = false) {
    const status = document.getElementById('editStatus');
    status.textContent = message;
    status.className = error ? 'red-text text-darken-2' : 'grey-text text-darken-2';
}

async function checkEditResponse(response) {
    if (response.ok && !response.redirected) return;
    let message = 'Nie udało się przetworzyć faktury. Spróbuj ponownie.';
    if (response.status === 403) message = 'Poprawianie faktur jest dostępne tylko dla administratora.';
    else if (response.status === 401 || response.redirected) message = 'Sesja wygasła. Zaloguj się ponownie.';
    else {
        const data = await response.json().catch(() => null);
        if (data?.message) message = data.message;
    }
    throw new Error(message);
}

async function initReceiptEdit() {
    document.getElementById('receiptEditForm').addEventListener('submit', submitReceiptEdit);
    await loadCurrentUser();
    if (!IS_ADMIN) {
        setEditStatus('Poprawianie faktur jest dostępne tylko dla administratora.', true);
        return;
    }
    if (!receiptId) {
        setEditStatus('Nie wskazano faktury. Wybierz fakturę z listy.', true);
        return;
    }
    try {
        const response = await fetch(`/api/v1/receipt/${encodeURIComponent(receiptId)}/edit`);
        await checkEditResponse(response);
        editedReceipt = await response.json();
        document.getElementById('receiptNumber').textContent = editedReceipt.number;
        document.getElementById('generateDate').value = editedReceipt.dateAndPlace.generateDate;
        document.getElementById('sellDate').value = editedReceipt.dateAndPlace.sellDate;
        const container = document.getElementById('editItems');
        container.replaceChildren();
        editedReceipt.items.forEach((item, index) => {
            const row = document.createElement('div');
            row.className = 'row receipt-edit-item';
            // Only static markup and the local index go into HTML; document text uses textContent.
            row.innerHTML = `
                <div class="col s12"><h6></h6></div>
                <div class="input-field col s12 m6">
                    <input id="amount-${index}" type="number" min="0" max="9999999999.99" step="0.01" required>
                    <label for="amount-${index}" class="active">Kwota sprzedaży netto (zł)</label>
                </div>
                <div class="col s12 m6">
                    <label for="vat-${index}">Stawka VAT</label>
                    <select id="vat-${index}" class="browser-default" required></select>
                </div>`;
            row.querySelector('h6').textContent = `${index + 1}. ${item.name}`;
            row.querySelector('input').value = Number(item.nettAmount).toFixed(2);
            const select = row.querySelector('select');
            Object.entries(editVatOptions).forEach(([code, label]) => {
                select.add(new Option(label, label, false, code === item.vatRate));
            });
            container.appendChild(row);
        });
        container.addEventListener('input', updateEditTotals);
        document.getElementById('receiptEditForm').classList.remove('hide');
        M.updateTextFields();
        updateEditTotals();
        setEditStatus('Zmień dane faktury i wybierz sposób pobrania.');
    } catch (error) {
        setEditStatus(error.message, true);
    }
}

function collectReceiptEdits() {
    return {
        generateDate: document.getElementById('generateDate').value,
        sellDate: document.getElementById('sellDate').value,
        items: editedReceipt.items.map((item, index) => ({
            id: item.id,
            nettAmount: document.getElementById(`amount-${index}`).value,
            vatRate: document.getElementById(`vat-${index}`).value
        }))
    };
}

function updateEditTotals() {
    let netCents = 0;
    let vatCents = 0;
    collectReceiptEdits().items.forEach(item => {
        const cents = Math.round(Number(item.nettAmount) * 100);
        if (!Number.isFinite(cents)) return;
        const rate = item.vatRate === 'ZW' ? 0 : Number(item.vatRate.replace('%', ''));
        // Match the PDF model's HALF_EVEN rounding, using integer cents.
        const numerator = cents * rate;
        const whole = Math.floor(numerator / 100);
        const remainder = numerator % 100;
        netCents += cents;
        vatCents += whole + (remainder > 50 || (remainder === 50 && whole % 2 !== 0) ? 1 : 0);
    });
    const money = cents => (cents / 100).toLocaleString('pl-PL', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    document.getElementById('totalNet').textContent = money(netCents);
    document.getElementById('totalVat').textContent = money(vatCents);
    document.getElementById('totalGross').textContent = money(netCents + vatCents);
}

async function submitReceiptEdit(event) {
    event.preventDefault();
    if (editInProgress || !editedReceipt || !IS_ADMIN) return;
    if (!document.getElementById('receiptEditForm').reportValidity()) return;
    const save = event.submitter?.value === 'save';
    const payload = collectReceiptEdits();
    editInProgress = true;
    document.getElementById('editFields').disabled = true;
    setEditStatus(save ? 'Zapisywanie i przygotowywanie PDF…' : 'Przygotowywanie PDF…');
    try {
        const response = await fetch(`/api/v1/receipt/${encodeURIComponent(receiptId)}/edit?save=${save}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        await checkEditResponse(response);
        const blob = await response.blob();
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `faktura_${editedReceipt.number.replace(/[^\w-]/g, '_')}.pdf`;
        document.body.appendChild(link);
        link.click();
        link.remove();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
        setEditStatus(save ? 'Zmiany zapisane. Pobrano poprawioną fakturę.' : 'Pobrano PDF. Zmiany nie zostały zapisane w systemie.');
    } catch (error) {
        setEditStatus(error.message, true);
    } finally {
        editInProgress = false;
        document.getElementById('editFields').disabled = false;
    }
}

document.addEventListener('DOMContentLoaded', initReceiptEdit);
