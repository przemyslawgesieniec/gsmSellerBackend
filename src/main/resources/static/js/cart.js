// ===========================
//  KOSZYK – FRONTEND LOGIC
// ===========================

let cartState = null;
let cartCount = 0;

/**
 * Pobiera koszyk użytkownika z backendu
 */
async function fetchCart() {
    try {
        const res = await fetch("/api/v1/cart");

        if (!res.ok) {
            throw new Error("Błąd pobierania koszyka");
        }

        return await res.json();

    } catch (err) {
        console.error(err);
        M.toast({ html: "Nie udało się pobrać koszyka", classes: "red" });
        return null;
    }
}

/**
 * Obsługa kliknięcia przycisku koszyka
 */
async function openCart() {
    console.log("🔵 Kliknięto koszyk");

    cartState = await fetchCart();
    if (!cartState) return;

    const phones = await fetchCartPhones();
    console.log("📱 Telefony w koszyku:", phones);

    renderCartCard(phones);

    M.Modal.getInstance(document.getElementById("cartModal")).open();
}

/**
 * Rendering karty koszyka
 */
function renderCartCard(phones) {
    const container = document.getElementById("cartContainer");
    if (!container) return;

    let html = `
        <h5 class="blue-text text-darken-2">Koszyk (${phones.length} telefonów)</h5>
        <div id="cartItemsList">
    `;

    if (phones.length === 0) {
        html += `<p>Koszyk jest pusty.</p>`;
    } else {

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
                        
                        <button class="btn-small red" 
                                onclick="removeFromCart('${phone.technicalId}')">
                            <i class="material-icons">close</i>
                        </button>
                    </div>
                </div>
            `;
        });
    }

    // --- PRZYCISK DODAJ POZYCJĘ RĘCZNIE ---
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

    setTimeout(() => {
        const priceInputs = document.querySelectorAll("input[id^='price-']");
        priceInputs.forEach(input => {
            input.addEventListener("input", updateCartTotal);
        });
        updateCartTotal();
    }, 0);

}

/**
 * Usuwanie telefonu z koszyka
 */
async function removeFromCart(technicalId) {
    try {
        const res = await fetch(`/api/v1/cart/${technicalId}`, { method: "DELETE" });

        if (!res.ok) throw new Error("Błąd usuwania");

        // pobierz pełne dane
        const phones = await fetchCartPhones();

        renderCartCard(phones);
        updateCartTotal();

        M.toast({ html: "Usunięto z koszyka", classes: "blue" });

    } catch (err) {
        console.error(err);
        M.toast({ html: "Błąd usuwania", classes: "red" });
    }
}

async function fetchCartPhones() {
    try {
        const res = await fetch("/api/v1/cart/phones");

        if (!res.ok) {
            throw new Error("Błąd pobierania danych urządzeń");
        }

        return await res.json(); // <- List<PhoneStockDto>

    } catch (err) {
        console.error(err);
        M.toast({ html: "Błąd pobierania telefonów z koszyka", classes: "red" });
        return [];
    }
}


function addManualItem() {
    const container = document.getElementById("cartItemsList");
    if (!container) return;

    const tempId = "manual-" + Date.now();

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
                <div class="input-field" style="margin-top:12px; max-width:130px; position:relative;">
                    <input type="number" id="price-${tempId}" class="validate">
                    <label class="active" for="price-${tempId}">Cena (zł)</label>
                </div>
                
                <button class="btn-small red" onclick="removeManualItem('${tempId}')">
                    <i class="material-icons">close</i>
                </button>
            </div>
        </div>
    `;

    // Dodaj przed przyciskiem +
    const addBtn = document.querySelector(".add-manual-item");
    container.insertBefore(
        document.createRange().createContextualFragment(html),
        addBtn
    );

    // podłącz aktualizację sumy
    const priceInput = document.getElementById(`price-${tempId}`);
    if (priceInput) {
        priceInput.addEventListener("input", updateCartTotal);
    }
    updateCartTotal();

}

function removeManualItem(id) {
    const el = document.getElementById(id);
    if (el) el.remove();
    updateCartTotal();
}

function updateCartTotal() {
    let sum = 0;

    // wszystkie pola input mają ID w formacie "price-xxx"
    const priceInputs = document.querySelectorAll("input[id^='price-']");

    priceInputs.forEach(input => {
        const value = parseFloat(input.value);
        if (!isNaN(value)) {
            sum += value;
        }
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
    var elems = document.querySelectorAll('.modal');
    M.Modal.init(elems);

    const cartBtn = document.getElementById("cartBtn");
    if (cartBtn) {
        cartBtn.addEventListener("click", openCart);
    }
});

