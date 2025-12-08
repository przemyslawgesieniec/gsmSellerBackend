
/**
 * Pobiera koszyk (ten sam endpoint co front koszyka)
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
 * Renderowanie faktury
 */
function renderInvoice(cart) {
    const tbody = document.getElementById("invoiceItems");
    const totalEl = document.getElementById("invoiceTotal");

    tbody.innerHTML = "";
    let total = 0;
    let counter = 1;

    cart.items.forEach(item => {
        const desc = item.description ?? `${item.name} ${item.model ?? ""}`;
        const price = item.sellingPrice ?? item.price ?? 0;

        total += parseFloat(price);

        let extraFields = "";

        // ============================
        // DODATKOWE POLA DLA TELEFONÓW
        // ============================
        if (item.itemType === "PHONE") {
            const gwId = `gw-${item.technicalId}`;
            const usedId = `used-${item.technicalId}`;

            extraFields = `
                <tr>
                    <td></td>
                    <td colspan="2" style="padding-top:10px;">

                        <div style="display:flex; align-items:center; gap:20px;">

                            <!-- Slider gwarancji -->
                            <div style="flex:1;">
                                <label for="${gwId}" style="font-weight:600;">Ile gwarancji (miesiące)</label>
                                <div style="display:flex; align-items:center; gap:15px;">
                                    <input 
                                        type="range" 
                                        id="${gwId}" 
                                        min="0" max="24" value="0"
                                        oninput="document.getElementById('${gwId}-num').value = this.value"
                                    >
                                    <input 
                                        type="number" id="${gwId}-num" min="0" max="24" value="0"
                                        style="width:60px;"
                                        oninput="document.getElementById('${gwId}').value = this.value"
                                    >
                                </div>
                            </div>

                            <!-- Checkbox używany -->
                            <div>
                                <label>
                                    <input type="checkbox" id="${usedId}">
                                    <span>Używany</span>
                                </label>
                            </div>

                        </div>

                    </td>
                </tr>
            `;
        }

        // POZYCJA GŁÓWNA
        tbody.innerHTML += `
            <tr>
                <td>${counter++}</td>
                <td>${desc}</td>
                <td>${price.toFixed(2)}</td>
            </tr>
            ${extraFields}
        `;
    });

    totalEl.textContent = total.toFixed(2);
}

/**
 * Opcjonalnie — dynamiczne ładowanie navbaru
 */
async function loadNavbar() {
    const navContainer = document.getElementById("navbar-container");
    const res = await fetch("navbar.html"); // jeżeli trzymasz navbar w osobnym pliku
    if (res.ok) {
        navContainer.innerHTML = await res.text();
        M.Sidenav.init(document.querySelectorAll('.sidenav'));
    }
}

document.addEventListener("DOMContentLoaded", async () => {
    await loadNavbar();

    // init dropdown
    M.FormSelect.init(document.querySelectorAll('select'));

    const cart = await fetchCart();
    if (cart) renderInvoice(cart);
});


function getSelectedVatBackend() {
    const vat = document.getElementById("vatSelect").value;
    return vat;
}

async function saveAndPrint() {
    const cart = await fetchCart();
    if (!cart) {
        M.toast({ html: "Brak danych koszyka", classes: "red" });
        return;
    }

    console.log("Koszyk zawiera : " + cart)

    const vat = getSelectedVatBackend();
    const items = collectInvoiceExtraData(cart);

    const payload = {
        vatRate: vat,
        items: items
    };

    console.log("📤 Wysyłam dane paragonu:", payload);

    try {
        // 1️⃣ Tworzymy dokument
        const res = await fetch("/api/v1/receipt", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });

        if (!res.ok) throw new Error("Błąd generowania dokumentu");

        const receiptUuid = await res.json();
        console.log("📥 Otrzymano receiptUuid:", receiptUuid);

        M.toast({ html: "Dokument zapisany — otwieram podgląd...", classes: "green" });

        // 2️⃣ Pobieramy PDF i otwieramy go użytkownikowi
        openReceiptPdf(receiptUuid);

    } catch (err) {
        console.error(err);
        M.toast({ html: "Błąd zapisywania dokumentu", classes: "red" });
    }
}

function collectInvoiceExtraData(cart) {
    return cart.items.map(item => {

        const isPhone = item.itemType === "PHONE";

        // -----------------------------
        // NORMALIZACJA CENY
        // -----------------------------
        const rawPrice =
            item.sellingPrice ??
            item.price ??
            0;

        const price = Number(rawPrice);

        // -----------------------------
        // NORMALIZACJA OPISU
        // (to pole teraz zastępuje name)
        // -----------------------------
        const baseDescription =
            item.description ??
            `${item.name ?? ""} ${item.model ?? ""}`.trim() ??
            "Pozycja";

        if (!isPhone) {
            // -----------------------------
            //  MISC
            // -----------------------------
            return {
                itemType: "MISC",
                technicalId: null,
                description: baseDescription,
                price: price,
                warrantyMonths: null,
                used: null
            };
        }

        // -----------------------------
        //  PHONE
        // -----------------------------
        const gwValue = document.getElementById(`gw-${item.technicalId}-num`)?.value;
        const usedValue = document.getElementById(`used-${item.technicalId}`)?.checked;

        const phoneDescription =
            `${item.name ?? ""} ${item.model ?? ""}`.trim() || baseDescription;

        return {
            itemType: "PHONE",
            technicalId: item.technicalId,
            warrantyMonths: Number(gwValue ?? 0),
            used: Boolean(usedValue),
            price: price,
            description: phoneDescription
        };
    });
}


function openReceiptPdf(receiptUuid) {
    const url = `/api/v1/receipt/${receiptUuid}`;
    window.open(url, "_blank");
}

