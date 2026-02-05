/**
 * =========================
 *  GLOBAL STATE
 * =========================
 */
let CURRENT_CART = null;

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
        M.toast({ html: "Nie udało się pobrać koszyka", classes: "red" });
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

    let totalNet = 0;
    let counter = 1;

    cart.items.forEach(item => {
        const desc =
            item.description ??
            `${item.name ?? ""} ${item.model ?? ""}`.trim();

        const price = Number(item.sellingPrice ?? item.price ?? 0);
        totalNet += price;

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
                        <textarea id="${sellingInfoId}" class="materialize-textarea" style="margin-bottom: 0;"></textarea>
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

    updateTotals(totalNet);
}

/**
 * =========================
 *  TOTALS
 * =========================
 */
function updateTotals(netTotal) {
    const vatValue = getSelectedVatBackend();
    const grossTotal = calculateGrossTotal(netTotal, vatValue);

    document.getElementById("totalNet").textContent = netTotal.toFixed(2);
    document.getElementById("invoiceTotal").textContent = grossTotal.toFixed(2);
    document.getElementById("totalVat").textContent =
        (grossTotal - netTotal).toFixed(2);
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

function calculateGrossTotal(netTotal, vatValue) {
    return netTotal * (1 + parseVatRate(vatValue));
}

/**
 * =========================
 *  SAVE + PRINT
 * =========================
 */
async function saveAndPrint() {
    if (!CURRENT_CART) {
        M.toast({ html: "Brak danych koszyka", classes: "red" });
        return;
    }

    const payload = {
        vatRate: getSelectedVatBackend(),
        sellDate: getSellDate(),
        items: collectInvoiceExtraData(CURRENT_CART),
        customerNote: document.getElementById("customerNote")?.value ?? ""
    };

    try {
        const res = await fetch("/api/v1/receipt", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });

        if (!res.ok) throw new Error();

        const receiptUuid = await res.json();
        M.toast({ html: "Dokument zapisany", classes: "green" });
        openReceiptPdf(receiptUuid);
        resetInvoiceForm();

    } catch {
        M.toast({ html: "Błąd zapisu dokumentu", classes: "red" });
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
        console.log("Item: " + item)

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
    const dp = M.Datepicker.getInstance(document.getElementById("sellDate"));
    if (dp) dp.setDate(new Date());
}

/**
 * =========================
 *  INIT (ONE TIME ONLY)
 * =========================
 */
document.addEventListener("DOMContentLoaded", async () => {

    // DATEPICKER (MOBILE SAFE)
    M.Datepicker.init(document.querySelectorAll(".datepicker"), {
        format: "yyyy-mm-dd",
        autoClose: true,
        firstDay: 1,
        yearRange: 5
    });

    // LOAD CART
    CURRENT_CART = await fetchCart();
    if (CURRENT_CART) renderInvoice(CURRENT_CART);

    // VAT CHANGE
    document
        .getElementById("vatSelect")
        ?.addEventListener("change", () => {
            renderInvoice(CURRENT_CART);
        });
});

new TomSelect('#vatSelect', {
    controlInput: null,
    create: false,
    allowEmptyOption: false
});

