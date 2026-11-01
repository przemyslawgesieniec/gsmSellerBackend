/**
 * =========================
 *  GLOBAL STATE
 * =========================
 */
let CURRENT_CART = null;
let invoiceSaving = false;
let manualItemCounter = 0;

/**
 * =========================
 *  FETCH CART
 * =========================
 */
async function fetchCart() {
    try {
        const res = await fetch("/api/v1/cart");
        if (!res.ok) throw new Error("Błąd pobierania koszyka");
        return await res.json();
    } catch (err) {
        console.error(err);
        UI.toast({ html: "Nie udało się pobrać koszyka", classes: "red" });
        return null;
    }
}

/**
 * =========================
 *  RENDER INVOICE
 * =========================
 */
function renderInvoice(cart) {
    if (!cart) return;

    const tbody = document.getElementById("invoiceItems");
    tbody.innerHTML = "";

    let counter = 1;

    cart.items.forEach(item => {
        const desc =
            item.description ??
            `${item.name ?? ""} ${item.model ?? ""}`.trim();

        const price = Number(item.sellingPrice ?? item.price ?? 0);

        let extraFields = "";

        // =========================
        // PHONE EXTRA FIELDS
        // =========================
        if (item.itemType === "PHONE") {
            const gwId = `gw-${item.technicalId}`;
            const usedId = `used-${item.technicalId}`;
            const sellingInfoId = `sellingInfo-${item.technicalId}`;

            extraFields = `
              <tr>
                <td></td>
                <td colspan="3">
                  <div style="display:flex; flex-direction:column; gap:10px;">

                    <div>
                      <label>Ile gwarancji (miesiące)</label>
                      <div style="display:flex; align-items:center; gap:10px;">
                        <input type="range"
                          id="${gwId}"
                          min="0" max="24" value="0"
                          oninput="document.getElementById('${gwId}-num').value=this.value"
                        >
                        <input type="number"
                          id="${gwId}-num"
                          min="0" max="24" value="0"
                          style="width:70px"
                          oninput="document.getElementById('${gwId}').value=this.value"
                        >
                      </div>
                    </div>

                    <div>
                        <label for="${sellingInfoId}" style="font-weight: bold; font-size: 1.2rem; color: red;">INFORMACJE DOTYCZĄCE SPRZEDAŻY (WIDOCZNE WYŁĄCZNIE W SYSTEMIE)</label>
                        <textarea id="${sellingInfoId}" class="text-area" style="margin-bottom: 0;"></textarea>
                    </div>

                    <label>
                      <input type="checkbox" id="${usedId}">
                      <span>Używany</span>
                    </label>

                  </div>
                </td>
              </tr>
            `;
        }

        tbody.insertAdjacentHTML("beforeend", `
          <tr>
            <td>${counter++}</td>
            <td>${desc}</td>
            <td>${price.toFixed(2)}</td>
          </tr>
          ${extraFields}
        `);
    });

    if (!cart.items.length) {
        tbody.innerHTML = '<tr><td colspan="3" class="grey-text">Koszyk jest pusty. Dodaj pozycję ręcznie poniżej.</td></tr>';
    }
    updateTotals();
}

/**
 * =========================
 *  TOTALS
 * =========================
 */
function updateTotals() {
    const rate = Math.round(parseVatRate(getSelectedVatBackend()) * 100);
    const prices = [
        ...(CURRENT_CART?.items ?? []).map(item => Number(item.sellingPrice ?? item.price ?? 0)),
        ...collectManualInvoiceItems().map(item => item.price)
    ];
    let netCents = 0;
    let vatCents = 0;
    prices.forEach(price => {
        if (!Number.isFinite(price) || price < 0) return;
        const cents = Math.round(price * 100);
        // The PDF rounds VAT per item using HALF_EVEN.
        const numerator = cents * rate;
        const whole = Math.floor(numerator / 100);
        const remainder = numerator % 100;
        netCents += cents;
        vatCents += whole + (remainder > 50 || (remainder === 50 && whole % 2 !== 0) ? 1 : 0);
    });
    document.getElementById("totalNet").textContent = (netCents / 100).toFixed(2);
    document.getElementById("totalVat").textContent = (vatCents / 100).toFixed(2);
    document.getElementById("invoiceTotal").textContent = ((netCents + vatCents) / 100).toFixed(2);
}

function addManualInvoiceItem() {
    const id = ++manualItemCounter;
    const row = document.createElement("div");
    row.className = "surface manual-invoice-item";
    row.innerHTML = `
        <div class="form-grid">
            <div class="field grid-cell span-s-12 span-m-8">
                <input id="manual-description-${id}" class="manual-description" type="text" required>
                <label for="manual-description-${id}">Opis pozycji</label>
            </div>
            <div class="field grid-cell span-s-12 span-m-4">
                <input id="manual-price-${id}" class="manual-price" type="number"
                       min="0.01" max="9999999999.99" step="0.01" required inputmode="decimal">
                <label for="manual-price-${id}">Cena netto (zł)</label>
            </div>
        </div>
        <button type="button" class="button-quiet red-text remove-manual-item">Usuń pozycję</button>`;
    const description = row.querySelector('.manual-description');
    const validateDescription = () => description.setCustomValidity(
        description.value.trim() ? '' : 'Wpisz opis pozycji.'
    );
    validateDescription();
    description.addEventListener('input', validateDescription);
    row.addEventListener('input', () => {
        UI.updateTextFields();
        updateTotals();
    });
    row.querySelector('.remove-manual-item').addEventListener('click', () => {
        row.remove();
        updateTotals();
        document.getElementById('addManualItemBtn').focus();
    });
    document.getElementById('manualInvoiceItems').appendChild(row);
    description.focus();
    updateTotals();
}

function collectManualInvoiceItems() {
    return Array.from(document.querySelectorAll('.manual-invoice-item'), row => ({
        itemType: 'MISC',
        technicalId: null,
        description: row.querySelector('.manual-description').value.trim(),
        price: Number(row.querySelector('.manual-price').value),
        warrantyMonths: null,
        used: null
    }));
}

/**
 * =========================
 *  VAT
 * =========================
 */
function getSelectedVatBackend() {
    return document.getElementById("vatSelect")?.value ?? "ZW";
}

function parseVatRate(vatValue) {
    if (!vatValue) return 0;
    if (vatValue.endsWith("%")) {
        return Number(vatValue.replace("%", "")) / 100;
    }
    return 0;
}

/**
 * =========================
 *  SAVE + PRINT
 * =========================
 */
async function saveAndPrint() {
    if (invoiceSaving) return;
    if (!CURRENT_CART) {
        UI.toast({ html: "Brak danych koszyka", classes: "red" });
        return;
    }

    if (!document.getElementById('invoiceForm').reportValidity()) return;
    const items = [...collectInvoiceExtraData(CURRENT_CART), ...collectManualInvoiceItems()];
    if (!items.length) {
        UI.toast({ html: "Dodaj przynajmniej jedną pozycję do faktury", classes: "red" });
        return;
    }

    const payload = {
        vatRate: getSelectedVatBackend(),
        sellDate: getSellDate(),
        items,
        customerNote: document.getElementById("customerNote")?.value ?? ""
    };

    invoiceSaving = true;
    document.getElementById('invoiceFields').disabled = true;
    try {
        const res = await fetch("/api/v1/receipt", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });

        if (!res.ok || res.redirected) throw new Error();

        const receiptUuid = await res.json();
        UI.toast({ html: "Dokument zapisany", classes: "green" });
        openReceiptPdf(receiptUuid);
        resetInvoiceForm();

    } catch {
        UI.toast({ html: "Błąd zapisu dokumentu", classes: "red" });
    } finally {
        invoiceSaving = false;
        document.getElementById('invoiceFields').disabled = false;
    }
}

/**
 * =========================
 *  EXTRA DATA
 * =========================
 */
function collectInvoiceExtraData(cart) {
    return cart.items.map(item => {
        const price = Number(item.sellingPrice ?? item.price ?? 0);
        const descTmp = item.description ??
                `${item.name ?? ""} ${item.model ?? ""}`.trim();

        if (item.itemType !== "PHONE") {
            return {
                itemType: "MISC",
                technicalId: null,
                description:descTmp,
                price,
                warrantyMonths: null,
                used: null
            };
        }

        return {
            itemType: "PHONE",
            technicalId: item.technicalId,
            description: descTmp,
            price,
            warrantyMonths:
                Number(document.getElementById(`gw-${item.technicalId}-num`)?.value ?? 0),
            used:
                Boolean(document.getElementById(`used-${item.technicalId}`)?.checked),
            sellingInfo:
                document.getElementById(`sellingInfo-${item.technicalId}`)?.value ?? ""
        };
    });
}

/**
 * =========================
 *  HELPERS
 * =========================
 */
function openReceiptPdf(uuid) {
    window.open(`/api/v1/receipt/${uuid}`, "_blank");
}

function getSellDate() {
    return document.getElementById("sellDate")?.value || null;
}

function resetInvoiceForm() {
    CURRENT_CART = { items: [] };
    document.getElementById('manualInvoiceItems').replaceChildren();
    document.getElementById('customerNote').value = '';
    renderInvoice(CURRENT_CART);
    const dp = UI.DateInput.getInstance(document.getElementById("sellDate"));
    if (dp) {
        dp.setDate(new Date());
        dp.setInputValue();
    }
    UI.updateTextFields();
}

/**
 * =========================
 *  INIT (ONE TIME ONLY)
 * =========================
 */
document.addEventListener("DOMContentLoaded", async () => {

    // DATEPICKER (MOBILE SAFE)
    UI.DateInput.init(document.querySelectorAll(".date-input"), {
        format: "yyyy-mm-dd",
        autoClose: true,
        firstDay: 1,
        yearRange: 5
    });

    document.getElementById('invoiceForm').addEventListener('submit', event => {
        event.preventDefault();
        saveAndPrint();
    });
    document.getElementById('addManualItemBtn').addEventListener('click', addManualInvoiceItem);
    // Recalculate without rebuilding inputs: keep phone details and manual edits.
    document.getElementById('vatSelect').addEventListener('change', updateTotals);

    CURRENT_CART = await fetchCart();
    if (CURRENT_CART) {
        renderInvoice(CURRENT_CART);
        document.getElementById('invoiceFields').disabled = false;
    }
});

new TomSelect('#vatSelect', {
    controlInput: null,
    create: false,
    allowEmptyOption: false
});
