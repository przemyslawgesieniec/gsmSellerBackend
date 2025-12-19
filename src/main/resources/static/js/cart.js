// ===========================
//  KOSZYK – FRONTEND LOGIC
// ===========================

let cartState = null;

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
                       <div class="price-edit">
                        <input type="number"
                               class="price-input"
                               data-technical-id="${phone.technicalId}"
                               value="${phone.sellingPrice}"
                               step="0.01"
                               min="0">
                    </div>
                        <label class="active" for="price-${phone.technicalId}">Cena (zł)</label>
                    </div>

                   <div class="cart-item-actions" style="display:flex; gap:6px;">

                    <button class="btn-small blue save-item-btn hidden"
                            data-technical-id="ID"
                            data-type="PHONE">
                        <i class="material-icons">save</i>
                    </button>
                
                    <div style="display:flex; gap:6px;">
                    
                        <button class="btn-small blue save-item-btn hidden"
                                data-technical-id="${phone.technicalId}"
                                data-type="PHONE">
                            <i class="material-icons">save</i>
                        </button>
                    
                        <button class="btn-small red"
                                onclick="removeFromCart('${phone.technicalId}')">
                            <i class="material-icons">close</i>
                        </button>
                    
                    </div>

                
                </div>

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

            <!-- LEWA STRONA -->
            <div style="flex-grow:1; margin-right:15px; line-height:1.4;">
                <div class="input-field" style="margin-top:12px;">
                    <input type="text"
                           class="desc-input"
                           data-technical-id="${item.technicalId}"
                           data-type="MISC"
                           value="${item.description}">
                    <label class="active">Opis</label>
                </div>
            </div>

            <!-- PRAWA STRONA -->
            <div style="display:flex; flex-direction:column; align-items:flex-end;">

                <div class="input-field" style="margin-top:12px; max-width:130px;">
                    <input type="number"
                           class="price-input"
                           data-technical-id="${item.technicalId}"
                           data-type="MISC"
                           value="${item.price}"
                           step="0.01"
                           min="0">
                    <label class="active">Cena (zł)</label>
                </div>

                <div class="cart-item-actions"
                     style="display:flex; gap:6px;">

                    <button class="btn-small blue save-item-btn hidden"
                            data-technical-id="${item.technicalId}"
                            data-type="MISC">
                        <i class="material-icons">save</i>
                    </button>

                    <button class="btn-small red"
                            onclick="removeFromCart('${item.technicalId}')">
                        <i class="material-icons">close</i>
                    </button>

                </div>

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
        const priceInputs = document.querySelectorAll(".price-input");
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
        <div class="z-depth-1"
             id="${tempId}"
             style="padding: 12px; border-radius: 8px; margin-bottom: 15px;
                    display:flex; justify-content:space-between; align-items:flex-start;">

            <!-- LEWA -->
            <div style="flex-grow:1; margin-right:15px;">
                <div class="input-field" style="margin-top:12px;">
                    <input type="text"
                           class="desc-input"
                           data-technical-id="${tempId}"
                           data-type="MISC">
                    <label class="active">Opis</label>
                </div>
            </div>

            <!-- PRAWA -->
            <div style="display:flex; flex-direction:column; align-items:flex-end;">

                <div class="input-field" style="margin-top:12px; max-width:130px;">
                    <input type="number"
                           class="price-input"
                           data-technical-id="${tempId}"
                           data-type="MISC"
                           step="0.01"
                           min="0">
                    <label class="active">Cena (zł)</label>
                </div>

                <div class="cart-item-actions"
                     style="display:flex; gap:6px;">

                    <!-- SAVE OD RAZU -->
                    <button class="btn-small blue save-item-btn"
                            data-technical-id="${tempId}"
                            data-type="MISC">
                        <i class="material-icons">save</i>
                    </button>

                    <button class="btn-small red"
                            onclick="removeManualItem('${tempId}')">
                        <i class="material-icons">close</i>
                    </button>

                </div>

            </div>
        </div>
    `;

    const addBtn = document.querySelector(".add-manual-item");
    container.insertBefore(
        document.createRange().createContextualFragment(html),
        addBtn
    );

    updateCartTotal();
}

function removeManualItem(id) {
    const el = document.getElementById(id);
    if (el) el.remove();
    updateCartTotal();
}

function updateCartTotal() {
    let sum = 0;

    const priceInputs = document.querySelectorAll(".price-input");

    priceInputs.forEach(input => {
        const value = parseFloat(input.value);
        if (!isNaN(value)) sum += value;
    });

    const totalEl = document.getElementById("cartTotal");
    if (totalEl) {
        totalEl.textContent = `Suma: ${sum.toFixed(2)} zł`;
    }
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

function goToInvoice() {
    window.location.href = "invoice.html";
}

document.addEventListener("input", (e) => {
    if (!e.target.classList.contains("price-input")) return;

    const technicalId = e.target.dataset.technicalId;

    const btn = document.querySelector(
        `.save-price-btn[data-technical-id="${technicalId}"]`
    );

    if (btn) {
        btn.classList.remove("hidden");
    }

    updateCartTotal(); // suma dalej liczona live
});

document.addEventListener("click", async (e) => {
    if (!e.target.classList.contains("save-price-btn")) return;

    const technicalId = e.target.dataset.technicalId;

    const input = document.querySelector(
        `.price-input[data-technical-id="${technicalId}"]`
    );

    const price = parseFloat(input.value);

    if (!price || price <= 0) {
        M.toast({ html: "Niepoprawna cena", classes: "red" });
        return;
    }

    try {
        const res = await fetch(`/api/v1/cart/item/${technicalId}/price`, {
            method: "PATCH",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ price })
        });

        if (!res.ok) throw new Error();

        cartState = await res.json();

        e.target.classList.add("hidden");

        M.toast({ html: "Cena zapisana", classes: "green" });

    } catch (err) {
        console.error(err);
        M.toast({ html: "Błąd zapisu ceny", classes: "red" });
    }
});


document.addEventListener("input", (e) => {
    const el = e.target;

    if (
        !el.classList.contains("price-input") &&
        !el.classList.contains("desc-input")
    ) return;

    const technicalId = el.dataset.technicalId;

    const saveBtn = document.querySelector(
        `.save-item-btn[data-technical-id="${technicalId}"]`
    );

    if (saveBtn) {
        saveBtn.classList.remove("hidden");
    }

    updateCartTotal();
});

document.addEventListener("click", async (e) => {
    const btn = e.target.closest(".save-item-btn");
    if (!btn) return;

    const technicalId = btn.dataset.technicalId;
    const type = btn.dataset.type;

    try {
        if (type === "PHONE") {
            const price = parseFloat(
                document.querySelector(
                    `.price-input[data-technical-id="${technicalId}"]`
                ).value
            );

            if (!price || price <= 0) {
                M.toast({ html: "Niepoprawna cena", classes: "red" });
                return;
            }

            await updateCartItemPrice(technicalId, price);
        }

        if (type === "MISC") {
            const desc = document.querySelector(
                `.desc-input[data-technical-id="${technicalId}"]`
            ).value.trim();

            const price = parseFloat(
                document.querySelector(
                    `.price-input[data-technical-id="${technicalId}"]`
                ).value
            );

            if (!desc || !price || price <= 0) {
                M.toast({ html: "Uzupełnij opis i cenę", classes: "red" });
                return;
            }

            await saveOrUpdateMiscItem(technicalId, desc, price);
        }

        btn.classList.add("hidden");
        M.toast({ html: "Zapisano zmiany", classes: "green" });

    } catch (err) {
        console.error(err);
        M.toast({ html: "Błąd zapisu", classes: "red" });
    }
});

async function updateCartItemPrice(technicalId, price) {
    const res = await fetch(`/api/v1/cart/item/${technicalId}/price`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ price })
    });

    if (!res.ok) throw new Error();

    cartState = await res.json();
}
async function saveOrUpdateMiscItem(technicalId, desc, price) {
    const res = await fetch("/api/v1/cart/add/misc", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
            technicalId,
            description: desc,
            price
        })
    });

    if (!res.ok) throw new Error();

    cartState = await res.json();
}
