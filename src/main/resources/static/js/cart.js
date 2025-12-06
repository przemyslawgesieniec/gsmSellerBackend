// ===========================
//  KOSZYK – FRONTEND LOGIC
// ===========================

let cartState = null;
const manualSaveTimeouts = {};

/**
 * Pobiera pełny widok koszyka (telefony + misc)
 */
async function fetchFullCartView() {
    try {
        const res = await fetch("/api/v1/cart/full");

        if (!res.ok) {
            throw new Error("Błąd pobierania zawartości koszyka");
        }

        return await res.json(); // FullCartViewDto

    } catch (err) {
        console.error(err);
        M.toast({ html: "Nie udało się pobrać koszyka", classes: "red" });
        return { phones: [], miscItems: [] };
    }
}

/**
 * Obsługa kliknięcia przycisku koszyka
 */
async function openCart() {
    console.log("🔵 Kliknięto koszyk");

    cartState = await fetchFullCartView();
    if (!cartState) return;

    console.log("📱 Telefony:", cartState.phones);
    console.log("📦 MISC:", cartState.miscItems);

    renderCartCard(cartState);

    M.Modal.getInstance(document.getElementById("cartModal")).open();
}

/**
 * Rendering kart koszyka (telefony + misc)
 */
function renderCartCard(data) {
    const container = document.getElementById("cartContainer");
    if (!container) return;

    let phones = data.phones || [];
    let misc = data.miscItems || [];

    let html = `
        <h5 class="blue-text text-darken-2">Koszyk (${phones.length + misc.length} pozycji)</h5>
        <div id="cartItemsList">
    `;

    // === TELEFONY ===
    phones.forEach(phone => {
        html += `
            <div class="z-depth-1"
                 style="padding: 12px; border-radius: 8px; margin-bottom: 15px;
                        display:flex; justify-content:space-between; align-items:flex-start;">

                <div style="line-height: 1.4;">
                    <b>${phone.name}</b><br>
                    Model: ${phone.model}<br>
                    Kolor: ${phone.color}<br>
                    RAM: ${phone.ram}<br>
                    Pamięć: ${phone.memory}<br>
                    IMEI: ${phone.imei}<br>
                </div>

                <div style="display:flex; flex-direction:column; align-items:flex-end;">
                    <div class="input-field" style="margin-top:12px; max-width:130px;">
                        <input type="number" 
                               id="price-${phone.technicalId}"
                               value="${phone.sellingPrice}"
                               class="validate">
                        <label class="active" for="price-${phone.technicalId}">Cena (zł)</label>
                    </div>

                    <button class="btn-small red" onclick="removeFromCart('${phone.technicalId}')">
                        <i class="material-icons">close</i>
                    </button>
                </div>
            </div>
        `;
    });

    // === MISC ITEMS ===
    misc.forEach(item => {
        html += `
        <div class="z-depth-1"
             style="padding: 12px; border-radius: 8px; margin-bottom: 15px;
                    display:flex; justify-content:space-between; align-items:flex-start;">

            <div style="flex-grow:1; margin-right:15px;">
                <div class="input-field" style="margin-top:12px;">
                    <input type="text" 
                           id="desc-${item.technicalId}" 
                           value="${item.description}"
                           class="validate">
                    <label class="active" for="desc-${item.technicalId}">Opis</label>
                </div>
            </div>

            <div style="display:flex; flex-direction:column; align-items:flex-end;">
                <div class="input-field" style="margin-top:12px; max-width:130px;">
                    <input type="number" 
                           id="price-${item.technicalId}" 
                           value="${item.price}"
                           class="validate">
                    <label class="active" for="price-${item.technicalId}">Cena (zł)</label>
                </div>

                <button class="btn-small red" onclick="removeFromCart('${item.technicalId}')">
                    <i class="material-icons">close</i>
                </button>
            </div>
        </div>
    `;
    });

    // -- Dodawanie MISC ręcznie --
    html += `
        <div class="z-depth-1 hoverable add-manual-item"
             onclick="addManualItem()"
             style="padding: 25px; border-radius: 8px; margin-bottom: 15px; 
                    display:flex; justify-content:center; align-items:center;
                    cursor:pointer; height:120px; background:#f2f2f2;">
            <i class="material-icons" style="font-size:48px; color:#b5b5b5;">add</i>
        </div>
    `;

    html += `</div>`;

    container.innerHTML = html;

    // Podpinanie eventów do aktualizacji sumy
    setTimeout(() => {
        const priceInputs = document.querySelectorAll("input[id^='price-']");
        priceInputs.forEach(input => input.addEventListener("input", updateCartTotal));
        updateCartTotal();
    }, 0);

    M.updateTextFields();
}

/**
 * Usuwanie pozycji (telefonu lub MISC)
 */
async function removeFromCart(technicalId) {
    try {
        const res = await fetch(`/api/v1/cart/${technicalId}`, { method: "DELETE" });

        if (!res.ok) throw new Error("Błąd usuwania");

        const data = await fetchFullCartView();

        renderCartCard(data);
        updateCartTotal();

        M.toast({ html: "Usunięto z koszyka", classes: "blue" });

    } catch (err) {
        console.error(err);
        M.toast({ html: "Błąd usuwania", classes: "red" });
    }
}

function addManualItem() {
    const container = document.getElementById("cartItemsList");
    if (!container) return;

    const tempId = generateUUID();

    const html = `
        <div class="z-depth-1 manual-item"
             id="${tempId}"
             style="padding: 12px; border-radius: 8px; margin-bottom: 15px;
                    display:flex; justify-content:space-between;">

            <div style="flex-grow:1; margin-right: 15px;">
                <div class="input-field" style="margin-top:12px;">
                    <input type="text" id="desc-${tempId}" class="validate">
                    <label class="active" for="desc-${tempId}">Opis</label>
                </div>
            </div>

            <div style="display:flex; flex-direction:column;">
                <div class="input-field" style="margin-top:12px; max-width:130px;">
                    <input type="number" id="price-${tempId}" class="validate">
                    <label class="active" for="price-${tempId}">Cena (zł)</label>
                </div>

                <button class="btn-small red" onclick="removeManualItem('${tempId}')">
                    <i class="material-icons">close</i>
                </button>
            </div>
        </div>
    `;

    const addBtn = document.querySelector(".add-manual-item");
    container.insertBefore(
        document.createRange().createContextualFragment(html),
        addBtn
    );

    const descInput = document.getElementById(`desc-${tempId}`);
    const priceInput = document.getElementById(`price-${tempId}`);

    // aktualizacja sumy
    priceInput.addEventListener("input", updateCartTotal);

    // debounced auto-save
    descInput.addEventListener("input", () => saveManualItemDebounced(tempId));
    priceInput.addEventListener("input", () => saveManualItemDebounced(tempId));

    updateCartTotal();
}

async function saveManualItem(tempId) {

    const desc = document.getElementById(`desc-${tempId}`).value.trim();
    const price = parseFloat(document.getElementById(`price-${tempId}`).value);

    if (!desc || isNaN(price) || price <= 0) {
        console.warn("⏳ Manual item not ready to send:", desc, price);
        return;
    }

    console.log("📤 Wysyłam MISC do backendu:", tempId, desc, price);

    try {
        const res = await fetch("/api/v1/cart/add/misc", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                description: desc,
                price: price,
                technicalId: tempId // backend może użyć albo wygenerować nowy UUID
            })
        });

        if (!res.ok) throw new Error("Błąd zapisu MISC");

        const updatedCart = await res.json();

        // znajdź prawdziwy technicalId nadany przez backend
        const realItem = updatedCart.items.find(i => i.description === desc && i.price == price);
        if (!realItem) {
            console.warn("⚠ Nie znaleziono nowego MISC w koszyku po zapisie");
            return;
        }

        const newId = realItem.technicalId;

        console.log("✅ Zapisano MISC, prawdziwy ID:", newId);

        // aktualizacja elementów DOM
        const el = document.getElementById(tempId);
        el.id = newId;

        document.getElementById(`desc-${tempId}`).id = `desc-${newId}`;
        document.getElementById(`price-${tempId}`).id = `price-${newId}`;

        // zmiana onclick w przycisku usuwania
        const deleteBtn = el.querySelector("button");
        deleteBtn.setAttribute("onclick", `removeFromCart('${newId}')`);

        // usuwamy timeout z cache
        delete manualSaveTimeouts[tempId];

        updateCartTotal();
        M.toast({ html: "Zapisano pozycję", classes: "green" });

    } catch (err) {
        console.error(err);
        M.toast({ html: "Błąd zapisu pozycji", classes: "red" });
    }
}


function removeManualItem(id) {
    const el = document.getElementById(id);
    if (el) el.remove();
    updateCartTotal();
}

function updateCartTotal() {
    let sum = 0;

    const priceInputs = document.querySelectorAll("input[id^='price-']");
    priceInputs.forEach(input => {
        const value = parseFloat(input.value);
        if (!isNaN(value)) sum += value;
    });

    const totalEl = document.getElementById("cartTotal");
    if (totalEl) {
        totalEl.textContent = `Suma: ${sum.toFixed(2)} zł`;
    }
}

function saveManualItemDebounced(tempId) {
    if (manualSaveTimeouts[tempId]) {
        clearTimeout(manualSaveTimeouts[tempId]);
    }

    // ustaw nowy timeout
    manualSaveTimeouts[tempId] = setTimeout(() => saveManualItem(tempId), 2000);
}

// ===========================
//  INICJALIZACJA
// ===========================

document.addEventListener("DOMContentLoaded", () => {
    M.Modal.init(document.querySelectorAll('.modal'));

    const cartBtn = document.getElementById("cartBtn");
    if (cartBtn) {
        cartBtn.addEventListener("click", openCart);
    }
});


function generateUUID() {
    return crypto.randomUUID
        ? crypto.randomUUID()
        : 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
            const r = Math.random() * 16 | 0;
            const v = c === 'x' ? r : (r & 0x3 | 0x8);
            return v.toString(16);
        });
}


