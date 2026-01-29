const pageSize = 20;
let currentPage = 0;
let phoneToHandover = null;

const listContainer = document.getElementById("phone-list");

function getFilters() {
    return {
        name: document.getElementById("filterName").value.trim(),
        model: document.getElementById("filterModel").value.trim(),
        color: document.getElementById("filterColor").value.trim(),
        imei: document.getElementById("filterImei").value.trim(),
        priceMin: document.getElementById("filterPriceMin").value.trim(),
        priceMax: document.getElementById("filterPriceMax").value.trim(),
        status: document.getElementById("filterStatus").value.trim(),
        locationName: document.getElementById("filterLocation").value.trim()
    };
}

async function loadStock(page = 0) {
    const filters = getFilters();

    try {
        renderFilterChips();

        const data = await fetchPhonesPage(page, filters);

        const phones = data.content.map(phone => ({
            technicalId: phone.technicalId,
            name: phone.name,
            model: phone.model,
            ram: phone.ram,
            memory: phone.memory,
            color: phone.color,
            imei: phone.imei,
            sellingPrice: phone.sellingPrice,
            purchasePrice: phone.purchasePrice,
            createDateTime: phone.createDateTime,
            status: phone.status,
            source: phone.source,
            locationName: phone.locationName,
            purchaseType: phone.purchaseType,
            comment: phone.comment,
            description: phone.description,
            batteryCondition: phone.batteryCondition,
            used: phone.used
        }));


        renderPhones(phones);
        initDropdowns();
        loadPagination(data.number, data.totalPages);
    } catch (error) {
        console.error("Błąd podczas pobierania danych:", error);
    }
    renderChips(filters);
}

async function fetchPhonesPage(page = 0, filters = {}) {
    try {
        const params = new URLSearchParams();

        params.append("page", page);
        params.append("size", pageSize);

        Object.entries(filters).forEach(([key, value]) => {
            if (value !== null && value !== "") {
                params.append(key, value);
            }
        });

        const response = await fetch(`/api/v1/phones?${params.toString()}`);

        if (!response.ok) {
            throw new Error(`HTTP error! Status: ${response.status}`);
        }

        return await response.json();
    } catch (error) {
        console.error("Błąd podczas pobierania telefonów:", error);
        M.toast({html: 'Błąd ładowania danych z serwera', classes: 'red'});
    }
}

function loadPagination(currentPage, totalPages) {
    const pagination = document.getElementById("dynamicPagination");
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

    if (totalPages <= 0) return;

    // ========== PRZYCISK "POPRAWDNIE" ==========
    if (currentPage > 0) {
        const leftIcon = '<i class="material-icons">chevron_left</i>';
        pagination.appendChild(createPageItem({
            classes: "waves-effect",
            inner: leftIcon,
            onClick: () => loadStock(currentPage - 1)
        }));
    } else {
        // disabled
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

    // generuj numery stron
    for (let i = startPage; i <= endPage; i++) {
        if (i === currentPage) {
            pagination.appendChild(createPageItem({
                classes: "active blue-grey darken-2",
                inner: String(i + 1),
                onClick: null // nie klikalna, bo aktywna
            }));
        } else {
            pagination.appendChild(createPageItem({
                classes: "waves-effect",
                inner: String(i + 1),
                onClick: () => loadStock(i)
            }));
        }
    }

    // ========== PRZYCISK "NASTĘPNA" ==========
    if (currentPage < totalPages - 1) {
        const rightIcon = '<i class="material-icons">chevron_right</i>';
        pagination.appendChild(createPageItem({
            classes: "waves-effect",
            inner: rightIcon,
            onClick: () => loadStock(currentPage + 1)
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

// Renderowanie kafli
const sellButtonIdPrefix = "sellBtn";
const editButtonIdPrefix = "editBtn";

const PURCHASE_TYPE_LABELS = {
    CASH: "Gotówka",
    VAT_23: "Faktura marża (ZW)",
    VAT_EXEMPT: "Faktura VAT (23%)"
};

function renderPhones(phones) {
    listContainer.innerHTML = "";
    phones.forEach((phone) => {
        const statusClass = phone.status;
        const inCart = isPhoneInCart(phone.technicalId);

        const disableSellButton =
            phone.status !== "DOSTĘPNY"
            || !phone.locationName
            || inCart
                ? "disabled"
                : "";


        const card = `
      <div class="col s12">
        <div
          class="card z-depth-2"
          data-technical-id="${phone.technicalId}"
          data-name="${phone.name ?? ""}"
          data-model="${phone.model ?? ""}"
          data-color="${phone.color ?? ""}"
          data-memory="${phone.memory ?? ""}"
          data-ram="${phone.ram ?? ""}"
          data-source="${phone.source ?? ""}"
          data-imei="${phone.imei ?? ""}"
          data-description="${phone.description ?? ""}"
          data-used="${phone.used ?? false}"
          data-battery-condition="${phone.batteryCondition ?? ""}"
          data-selling-price="${phone.sellingPrice ?? ""}"
          data-purchase-price="${phone.purchasePrice ?? ""}"
        >
          <div class="card-content phone-card">
            <div class="phone-left">
                <div class="title-row">
                  <span class="card-title">${phone.name}</span>
                
                  <span class="condition-badge ${phone.used ? "USED" : "NEW"}">
                    ${phone.used ? "UŻYWANY" : "NOWY"}
                  </span>
                </div>
                <p><i class="material-icons tiny">smartphone</i> <b>Model:</b> ${phone.model}</p>

                ${(phone.ram != null || phone.memory != null) ? `
                <p>
                  <i class="material-icons tiny">memory</i>
                  <b>Specyfikacja:</b>
                  ${phone.ram != null ? `${phone.ram}` : ``}
                  ${phone.ram != null && phone.memory != null ? ` / ` : ``}
                  ${phone.memory != null ? `${phone.memory}` : ``}
                </p>
                ` : ``}
                
                ${phone.source != null && phone.source !== "" ? `
                <p>
                  <i class="material-icons tiny">local_shipping</i>
                  <b>Pochodzenie:</b> ${phone.source}
                </p>
                ` : ``}

                <p><i class="material-icons tiny">palette</i> <b>Kolor:</b> ${phone.color}</p>
                
                <p><i class="material-icons tiny">receipt</i> <b>Forma zakupu:</b>
                  ${PURCHASE_TYPE_LABELS[phone.purchaseType] ?? "—"}
                </p>
                
                <p><i class="material-icons tiny">fingerprint</i> <b>IMEI:</b> ${phone.imei}</p>
                
                <p><i class="material-icons tiny">event</i> <b>Dodano:</b> ${phone.createDateTime}</p>
                
                ${phone.description? `<p> <i class="material-icons tiny">notes</i>  <b>Opis:</b> ${phone.description}</p>` : ``}
                
                ${phone.comment != null && phone.comment !== "" ? `
                <p>
                  <i class="material-icons tiny">comment</i>
                  <b>Komentarz:</b> ${phone.comment}
                </p>
                ` : ``}               
                ${phone.batteryCondition ? `
                <p class="battery-row">
                  <i class="material-icons tiny ${getBatteryIconClass(phone.batteryCondition)}">
                    ${getBatteryIcon(phone.batteryCondition)}
                  </i>
                  <b>Stan baterii:</b> ${phone.batteryCondition} %
                </p>
                ` : ``}
              ${
            phone.locationName
                ? `<p class="location">
               <i class="material-icons tiny blue-text">place</i>
               <b>${phone.locationName}</b>
             </p>`
                : `<p class="location grey-text">
               <i class="material-icons tiny">place</i>
               Brak lokalizacji
             </p>`
        }
            </div>
            <div class="phone-right">
            <div class="price-wrapper col s12">
                <p class="sellingPrice">${phone.sellingPrice} zł</p>
                <p class="initialPrice-hover">Zakup: ${phone.purchasePrice} zł</p>
            </div>          
              <span class="status-badge ${statusClass}">${phone.status}</span>
            </div>
          </div>
         <div class="card-action right-align">  
              <a class='dropdown-trigger btn-small blue darken-2' href='#' data-target='dropdown-${phone.technicalId}'>
                <i class="material-icons left">more_vert</i>Więcej
              </a>
            
            <ul id="dropdown-${phone.technicalId}" class="dropdown-content">
            
              <li>
                <a href="#!" data-action="edit" data-id="${phone.technicalId}">
                  Edytuj
                </a>
              </li>
            
              <li>
                <a href="#!"
                   data-action="accept"
                   data-id="${phone.technicalId}"
                   class="${phone.status !== 'WPROWADZONY' ? 'disabled-link' : ''}">
                  Przyjmij na sklep
                </a>
              </li>
            
             <li>
              <a href="#!"
                 data-action="remove-from-location"
                 data-id="${phone.technicalId}"
                 class="${phone.locationName && phone.status === 'DOSTĘPNY'
            ? ''
            : 'disabled-link'}">
                Usuń z lokalizacji
              </a>
            </li>
            <li>
              <a href="#!"
                 data-action="handover"
                 data-id="${phone.technicalId}"
                 class="${phone.status !== 'DOSTĘPNY' ? 'disabled-link' : ''}">
                Przekaż
              </a>
            </li>
              <li class="red-text">
                <a href="#!"
                   data-action="delete"
                   data-id="${phone.technicalId}"
                   class="${!['WPROWADZONY', 'DOSTĘPNY'].includes(phone.status)
            ? 'disabled-link'
            : ''}">
                  Usuń
                </a>
              </li>

              ${IS_ADMIN && phone.status === 'USUNIĘTY' ? `
              <li>
                <a href="#!" data-action="restore" data-id="${phone.technicalId}">
                  Przywróć
                </a>
              </li>
              ` : ''}
            
            </ul>
            
              <button class="btn-small green darken-2 ${disableSellButton}"
                 onclick="sellPhone('${phone.technicalId}')"
                ${disableSellButton ? "disabled" : ""}>
                <i class="material-icons left">attach_money</i>
                ${inCart ? "W koszyku" : "Sprzedaj"}
              </button>

        </div>
        </div>
      </div>
    `;

        const sellButtonId = sellButtonIdPrefix + phone.technicalId;
        listContainer.innerHTML += card;
    });

    M.Dropdown.init(document.querySelectorAll('.dropdown-trigger'), {
        closeOnClick: false,
        constrainWidth: false,
        coverTrigger: false
    });

}

let cartBtn = null;

function initDropdowns() {
    const elems = document.querySelectorAll('.dropdown-trigger');
    M.Dropdown.init(elems, {
        constrainWidth: false,
        coverTrigger: false,
        alignment: 'right'
    });
}

function acceptPhone(technicalId) {
    const card = document.querySelector(`.card[data-technical-id="${technicalId}"]`);
    if (!card) return;

    const status = card.querySelector(".status-badge")?.textContent.trim();

    if (status !== "WPROWADZONY") {
        M.toast({html: "Telefon nie może zostać przyjęty na sklep", classes: "red"});
        return;
    }

    const confirmBtn = document.getElementById("confirmAcceptYes");
    confirmBtn.setAttribute("data-technical-id", technicalId);

    // otwórz modal
    const modal = M.Modal.getInstance(document.getElementById("acceptConfirmModal"));
    modal.open();
}


async function sellPhone(technicalId) {
    if (isPhoneInCart(technicalId)) {
        return;
    }

    const btn = document.getElementById("sellBtn" + technicalId);
    if (btn) {
        btn.disabled = true;
        btn.classList.add("grey");
        btn.innerText = "W koszyku";
    }

    try {
        // opcjonalnie: zablokuj przycisk na czas requestu
        const btn = document.querySelector(`button[data-technical-id="${technicalId}"]`);
        if (btn) {
            btn.disabled = true;
        }

        const params = new URLSearchParams({technicalId});

        const response = await fetch(`/api/v1/cart/add?${params.toString()}`, {
            method: 'POST',
            headers: {
                'X-Requested-With': 'XMLHttpRequest'
                // jeśli używasz CSRF, trzeba tu dodać nagłówek z tokenem
            }
        });

        if (response.status === 409) {
            M.toast({ html: 'Telefon nie jest w twojej lokalizacji', classes: 'red' });

            // cofamy UI
            const btn = document.getElementById("sellBtn" + technicalId);
            if (btn) {
                btn.disabled = false;
                btn.innerText = "Sprzedaj";
                btn.classList.remove("grey");
            }
            return;
        }


        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }


        const updatedCart = await response.json();
        cartState = updatedCart;
        loadStock(currentPage);

        // 🔢 aktualizacja liczby w koszyku – dostosuj do swojego modelu Cart
        // Zakładam np. że masz: cart.items albo cart.phones
        const items = updatedCart.items || updatedCart.phones || [];
        cartCount = items.length;

        // Jeśli masz gdzieś licznik w UI, np. badge przy ikonce koszyka:
        const cartCountBadge = document.getElementById('cartCountBadge');
        if (cartCountBadge) {
            cartCountBadge.textContent = cartCount;
        }

        M.toast({html: 'Telefon dodany do koszyka', classes: 'green'});

        // opcjonalnie: zmień wygląd przycisku na „W koszyku”
        if (btn) {
            btn.textContent = 'W koszyku';
            btn.classList.remove('orange', 'darken-2');
            btn.classList.add('grey');
        }

    } catch (e) {
        console.error('Error adding to cart:', e);
        M.toast({html: 'Nie udało się dodać do koszyka', classes: 'red'});

        // przy błędzie pozwól znowu kliknąć
        const btn = document.querySelector(`button[data-technical-id="${technicalId}"]`);
        if (btn) {
            btn.disabled = false;
        }
    }
}

document.addEventListener('DOMContentLoaded', async () => {
    try {
        const res = await fetch('/api/v1/cart', {headers: {'X-Requested-With': 'XMLHttpRequest'}});
        if (res.ok) {
            const cart = await res.json();
            cartState = cart;
            const items = cart.items || cart.phones || [];
            cartCount = items.length;

            const cartCountBadge = document.getElementById('cartCountBadge');
            if (cartCountBadge) {
                cartCountBadge.textContent = cartCount;
            }
        }
    } catch (e) {
        console.warn('Nie udało się pobrać koszyka przy starcie:', e);
    }
});

function createCartButton() {
    cartBtn = document.createElement('div');
    cartBtn.id = 'cartBtn';
    cartBtn.className = 'btn-floating btn-large blue';
    cartBtn.innerHTML = `
        <i class="material-icons">shopping_cart</i>
        <span id="cartBadge" class="badge">0</span>
    `;

    document.body.appendChild(cartBtn);
}

document.addEventListener("DOMContentLoaded", function () {
    // Inicjalizacja modali
    loadStock(0);

    const dropArea = document.getElementById('dropArea');
    const fileInput = document.getElementById('fileInput');

    // Kliknięcie na dropArea otwiera wybór plików
    dropArea.addEventListener('click', () => fileInput.click());

    // Obsługa przeciągania plików
    dropArea.addEventListener('dragover', (e) => {
        e.preventDefault();
        dropArea.classList.add('dragover');
    });

    dropArea.addEventListener('dragleave', () => dropArea.classList.remove('dragover'));
    dropArea.addEventListener('drop', (e) => {
        e.preventDefault();
        dropArea.classList.remove('dragover');
        const files = e.dataTransfer.files;
        handleFiles(files);
    });

    // Obsługa wybranych plików z dialogu
    fileInput.addEventListener('change', () => handleFiles(fileInput.files));

    function handleFiles(files) {
        console.log("Wybrane pliki:", files);
        M.toast({html: `${files.length} plików wybranych`, classes: 'green'});
        // Tutaj później będzie logika przesyłania do backendu
    }

    createCartButton();
    [
        "filterName",
        "filterModel",
        "filterColor",
        "filterImei",
        "filterPriceMin",
        "filterPriceMax",
        "filterStatus",
        "filterLocation"
    ].forEach(id => {
        const el = document.getElementById(id);
        if (!el) return;
        el.addEventListener("input", liveReload);
        el.addEventListener("change", liveReload);
    });

    loadLocationsFilter();

    initSimpleSelect('#filterStatus');
    initSimpleSelect('#filterLocation');
});

function editPhone(technicalId) {
    try {
        const card = document.querySelector(
            `.card[data-technical-id="${technicalId}"]`
        );
        if (!card) return;

        document.getElementById("editName").value =
            card.dataset.name || "";

        document.getElementById("editModel").value =
            card.dataset.model || "";

        document.getElementById("editColor").value =
            card.dataset.color || "";

        document.getElementById("editImei").value =
            card.dataset.imei || "";

        document.getElementById("editPrice").value =
            card.dataset.sellingPrice || "";

        document.getElementById("editDescription").value =
            card.dataset.description || "";

        document.getElementById("editUsed").checked =
            card.dataset.used === "true";

        document.getElementById("editBatteryCondition").value =
            card.dataset.batteryCondition || "";

        // 🔐 ADMIN ONLY – purchasePrice
        if (IS_ADMIN) {
            document.getElementById("editPurchasePrice").value =
                card.dataset.purchasePrice || "";
        }

        M.updateTextFields();
        updateEditBatteryVisibility();

        document
            .getElementById("saveEditBtn")
            .setAttribute("data-technical-id", technicalId);

        M.Modal.getInstance(
            document.getElementById("editPhoneModal")
        ).open();

    } catch (e) {
        console.error(e);
        M.toast({ html: "Błąd edycji telefonu", classes: "red" });
    }
}

["editName", "editModel", "editUsed"].forEach(id => {
    document.getElementById(id).addEventListener("input", updateEditBatteryVisibility);
    document.getElementById(id).addEventListener("change", updateEditBatteryVisibility);
});


function shouldShowBatteryCondition(name, model, used) {
    const text = `${name} ${model}`.toLowerCase();
    return used === true && text.includes("iphone");
}

function updateEditBatteryVisibility() {
    const name = document.getElementById("editName").value || "";
    const model = document.getElementById("editModel").value || "";
    const used = document.getElementById("editUsed").checked;

    const wrapper = document.getElementById("editBatteryConditionWrapper");
    const input = document.getElementById("editBatteryCondition");

    if (shouldShowBatteryCondition(name, model, used)) {
        wrapper.classList.remove("hide");
    } else {
        wrapper.classList.add("hide");
        input.value = "";
    }

    M.updateTextFields();
}


document.getElementById("saveEditBtn").addEventListener("click", async () => {
    const technicalId =
        document.getElementById("saveEditBtn")
            .getAttribute("data-technical-id");

    const name = document.getElementById("editName").value.trim();
    const model = document.getElementById("editModel").value.trim();
    const used = document.getElementById("editUsed").checked;

    const batteryInput = document.getElementById("editBatteryCondition");
    const shouldSendBattery =
        shouldShowBatteryCondition(name, model, used);

    const updatedPhone = {
        name: name,
        model: model,
        color: document.getElementById("editColor").value.trim(),
        imei: document.getElementById("editImei").value.trim(),
        sellingPrice: parseFloat(
            document.getElementById("editPrice").value
        ),
        description: document.getElementById("editDescription").value.trim(),
        used: used,
        purchasePrice: document.getElementById("editPurchasePrice").value,
        batteryCondition:
            shouldSendBattery && batteryInput.value !== ""
                ? batteryInput.value
                : null
    };

    console.log("PATCH payload:", updatedPhone);

    try {
        const response = await fetch(`/api/v1/phones/${technicalId}`, {
            method: "PATCH",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(updatedPhone)
        });

        if (!response.ok) {
            throw new Error("Nie udało się zapisać zmian");
        }

        M.toast({
            html: "Telefon został zaktualizowany",
            classes: "green"
        });

        const modal = M.Modal.getInstance(
            document.getElementById("editPhoneModal")
        );
        modal.close();

        loadStock(currentPage);

    } catch (error) {
        console.error("Błąd podczas zapisu:", error);
        M.toast({ html: "Błąd podczas zapisu", classes: "red" });
    }
});

document.getElementById("resetFilters").addEventListener("click", () => {
    document.getElementById("filterName").value = "";
    document.getElementById("filterModel").value = "";
    document.getElementById("filterColor").value = "";
    document.getElementById("filterImei").value = "";
    document.getElementById("filterPriceMin").value = "";
    document.getElementById("filterPriceMax").value = "";

    const statusSelect = TomSelect.getInstance('#filterStatus');
    const locationSelect = TomSelect.getInstance('#filterLocation');

    statusSelect?.clear();
    locationSelect?.clear();

    renderFilterChips();
    loadStock(0);
});


// tłumaczenia etykiet
const filterLabels = {
    name: "Nazwa",
    model: "Model",
    color: "Kolor",
    imei: "IMEI",
    priceMin: "Cena od",
    priceMax: "Cena do",
    status: "Status",
    locationName: "Lokalizacja"
};

// === CHIPS SECTION === //
const chipContainer = document.getElementById("activeChips");

// generowanie chipów
function renderChips(filters) {

    if (!chipContainer) return;

    chipContainer.innerHTML = "";

    Object.entries(filters).forEach(([key, val]) => {
        if (!val) return;

        // 👇 mapowanie technicznej wartości na nazwę dla UI
        let displayValue = val;

        if (key === "locationName" && val === "__NO_LOCATION__") {
            displayValue = "Brak lokalizacji";
        }

        const chip = document.createElement("div");
        chip.className = "chip";

        chip.innerHTML = `
            ${filterLabels[key]}: <b>${displayValue}</b>
            <i class="close material-icons" data-filter="${key}">close</i>
        `;

        chipContainer.appendChild(chip);
    });

    // usuwanie chipa → czyści filtr → reload
    chipContainer.querySelectorAll(".close").forEach(icon => {
        icon.addEventListener("click", () => {
            const filterKey = icon.dataset.filter;

            const inputId = "filter" + capitalize(filterKey);
            const el = document.getElementById(inputId);

            if (el) {
                el.value = "";

                // // re-init selectów (status, lokalizacja)
                // if (el.tagName === "SELECT") {
                //     M.FormSelect.init(el);
                // }
            }

            loadStock(0);
        });
    });
}

function capitalize(str) {
    return str.charAt(0).toUpperCase() + str.slice(1);
}


function renderFilterChips() {
    const filters = getFilters();
    const container = document.getElementById("activeFiltersChips");
    container.innerHTML = "";

    // jawne mapowanie key → inputId
    const filterInputMap = {
        name: "filterName",
        model: "filterModel",
        color: "filterColor",
        imei: "filterImei",
        priceMin: "filterPriceMin",
        priceMax: "filterPriceMax",
        status: "filterStatus",
        locationName: "filterLocation"
    };

    Object.entries(filters).forEach(([key, value]) => {
        if (!value) return;

        let displayValue = value;

        // 📍 specjalny przypadek: brak lokalizacji
        if (key === "locationName" && value === "__NO_LOCATION__") {
            displayValue = "Nie przyjęty";
        }

        const chip = document.createElement("div");
        chip.className = "chip";
        chip.innerHTML = `
            ${filterLabels[key]}: ${displayValue}
            <i class="close material-icons">close</i>
        `;

        chip.querySelector("i").addEventListener("click", () => {
            const inputId = filterInputMap[key];
            const el = document.getElementById(inputId);

            if (el) {
                el.value = "";

                // // re-init selectów
                // if (el.tagName === "SELECT") {
                //     M.FormSelect.init(el);
                // }
            }

            renderFilterChips();
            loadStock(0);
        });

        container.appendChild(chip);
    });
}

// Kliknięcie "TAK" w potwierdzeniu
document.getElementById("confirmAcceptYes").addEventListener("click", async () => {
    const technicalId = document
        .getElementById("confirmAcceptYes")
        .getAttribute("data-technical-id");

    try {
        const response = await fetch(`/api/v1/phones/${technicalId}/accept`, {
            method: "POST"
        });

        if (!response.ok) {
            throw new Error("Błąd podczas przyjmowania na sklep");
        }

        M.toast({html: "Telefon przyjęty na sklep", classes: "green"});

        // Zamknij modale
        M.Modal.getInstance(document.getElementById("acceptConfirmModal")).close();

        // Odśwież listę
        loadStock(currentPage);

    } catch (error) {
        console.error(error);
        M.toast({html: "Nie udało się przyjąć telefonu", classes: "red"});
    }
});

async function loadLocationsFilter() {
    try {
        const res = await fetch("/api/v1/locations");
        if (!res.ok) return;

        const locations = await res.json();
        const select = document.getElementById("filterLocation");

        if (!select || !select.tomselect) {
            console.warn("Tom Select nie jest jeszcze zainicjalizowany");
            return;
        }

        const ts = select.tomselect;

        // czyścimy opcje (tylko dynamiczne)
        ts.clearOptions();

        // opcje bazowe
        ts.addOption({ value: "", text: "Dowolna" });
        ts.addOption({ value: "__NO_LOCATION__", text: "Brak lokalizacji" });

        // opcje z backendu
        locations.forEach(loc => {
            ts.addOption({
                value: loc.name,
                text: loc.name
            });
        });

        ts.refreshOptions(false);

    } catch (e) {
        console.warn("Nie udało się pobrać lokalizacji", e);
    }
}

function isPhoneInCart(technicalId) {
    if (!cartState || !cartState.items) return false;

    return cartState.items.some(item =>
        item.technicalId === technicalId && item.itemType === "PHONE"
    );
}

function debounce(fn, delay = 300) {
    let timeout;
    return (...args) => {
        clearTimeout(timeout);
        timeout = setTimeout(() => fn(...args), delay);
    };
}



const liveReload = debounce(() => loadStock(0), 300);


let phoneToDelete = null;

function confirmDeletePhone(technicalId) {
    phoneToDelete = technicalId;
    const modal = M.Modal.getInstance(
        document.getElementById("deletePhoneModal")
    );
    modal.open();
}

document.getElementById("confirmDeletePhoneBtn")
    .addEventListener("click", () => {
        fetch(`/api/v1/phones/${phoneToDelete}`, {
            method: "DELETE"
        })
            .then(resp => {
                if (!resp.ok) {
                    console.log("DELETE resp not ok: " + resp)
                    return resp.text().then(t => { throw new Error(t); });
                }

                M.toast({
                    html: "Telefon oznaczony jako usunięty",
                    classes: "green"
                });

                loadStock(currentPage);
            })
            .catch(err => {
                M.toast({
                    html: "Nie można usunąć telefonu",
                    classes: "red"
                });
            })
            .finally(() => {
                M.Modal.getInstance(
                    document.getElementById("deletePhoneModal")
                ).close();
            })
    });

document.addEventListener("touchstart", handleDropdownAction, { passive: false });
document.addEventListener("click", handleDropdownAction);


function handleDropdownAction(e) {
    const el = e.target.closest("a[data-action]");
    if (!el) return;

    e.preventDefault();
    e.stopImmediatePropagation(); // 🔥 WAŻNE

    const action = el.dataset.action;
    const id = el.dataset.id;

    // ręcznie zamykamy dropdown
    const dropdown = el.closest(".dropdown-content");
    const instance = M.Dropdown.getInstance(
        document.querySelector(`[data-target="${dropdown.id}"]`)
    );
    if (instance) instance.close();

    switch (action) {
        case "edit":
            editPhone(id);
            break;

        case "accept":
            acceptPhone(id);
            break;

        case "remove-from-location":
            removePhoneFromLocation(id);
            break;

        case "delete":
            confirmDeletePhone(id);
            break;

        case "restore":
            restorePhone(id);
            break;

        case "handover":
            openHandoverModal(id);
            break;

    }
}

function openHandoverModal(technicalId) {
    phoneToHandover = technicalId;

    document.getElementById("handoverComment").value = "";
    document.getElementById("handoverPrice").value = "";
    M.updateTextFields();

    const modal = M.Modal.getInstance(
        document.getElementById("handoverPhoneModal")
    );
    modal.open();
}


function removePhoneFromLocation(technicalId) {
    fetch(`/api/v1/phones/${technicalId}/remove-from-location`, {
        method: "POST"
    })
        .then(res => {
            if (!res.ok) throw new Error("Błąd usuwania z lokalizacji");
            M.toast({ html: "Telefon usunięty z lokalizacji", classes: "green" });
            return loadStock(currentPage);
        })
        .catch(err => {
            M.toast({ html: err.message });
        });
}

function restorePhone(technicalId) {
    fetch(`/api/v1/phones/${technicalId}/restore`, {
        method: "POST"
    })
        .then(res => {
            if (!res.ok) throw new Error("Błąd przywracania telefonu");
            M.toast({ html: "Telefon został przywrócony", classes: "green" });
            return loadStock(currentPage);
        })
        .catch(err => {
            M.toast({ html: err.message, classes: "red" });
        });
}

document.getElementById("confirmHandoverBtn")
    .addEventListener("click", async () => {

        const comment =
            document.getElementById("handoverComment").value;

        const price =
            document.getElementById("handoverPrice").value;

        try {
            const response = await fetch(
                `/api/v1/phones/${phoneToHandover}/handover`,
                {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({ comment, price })
                }
            );

            if (!response.ok) {
                throw new Error("Błąd przekazania");
            }

            M.toast({ html: "Telefon przekazany", classes: "green" });

            const modal = M.Modal.getInstance(
                document.getElementById("handoverPhoneModal")
            );
            modal.close();

            loadStock(); // refresh listy

        } catch (err) {
            console.error(err);
            M.toast({ html: "Błąd przekazania", classes: "red" });
        }
    });


function initSimpleSelect(selector) {
    const el = document.querySelector(selector);
    if (!el) return;

    new TomSelect(el, {
        controlInput: null,
        create: false,
        searchField: [],
        shouldSort: false,
        hideSelected: true,
        closeAfterSelect: true,
        allowEmptyOption: true
    });
}

function getBatteryIcon(percent) {
    const p = Number(percent);

    if (p >= 90) return "battery_full";
    if (p >= 80) return "battery_6_bar";
    if (p >= 70) return "battery_5_bar";
    if (p >= 60) return "battery_4_bar";
    if (p >= 50) return "battery_3_bar";
    if (p >= 30) return "battery_2_bar";
    if (p >= 15) return "battery_1_bar";

    return "battery_alert";
}

function getBatteryIconClass(percent) {
    const p = Number(percent);

    if (p >= 80) return "battery-good";
    if (p >= 50) return "battery-ok";
    if (p >= 30) return "battery-warn";

    return "battery-bad";
}
