const pageSize = 5;
let currentPage = 0;

const backendPath = `http://${window.location.hostname}:8090`;
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
        locationName: document.getElementById("filterLocation").value.trim() // 👈
    };
}

async function loadStock(page = 0) {
    try {
        const filters = getFilters();
        renderFilterChips();

        const data = await fetchPhonesPage(page, filters);

        const phones = data.content.map(phone => ({
            technicalId: phone.technicalId,
            name: phone.name,
            model: phone.model,
            color: phone.color,
            imei: phone.imei,
            sellingPrice: phone.sellingPrice,
            purchasePrice: phone.purchasePrice,
            status: phone.status,
            locationName: phone.locationName
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

        const response = await fetch(`${backendPath}/api/v1/phones?${params.toString()}`);

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

function renderPhones(phones) {
    listContainer.innerHTML = "";
    phones.forEach((phone) => {
        const statusClass = phone.status;
        const disableSellButton = phone.status === ("SPRZEDANY" || "USUNIĘTY" || "ODDANY") ? "disabled" : "";
        const sellButtonId = sellButtonIdPrefix + phone.technicalId;

        const card = `
      <div class="col s12">
        <div class="card z-depth-2" data-technical-id="${phone.technicalId}">
          <div class="card-content phone-card">
            <div class="phone-left">
              <span class="card-title">${phone.name}</span>
              <p><b>Model:</b> ${phone.model}</p>
              <p><b>Kolor:</b> ${phone.color}</p>
              <p><b>IMEI:</b> ${phone.imei}</p>
              ${
            phone.locationName
                ? `<p class="location">
               <i class="material-icons tiny red-text">place</i>
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
            
              <ul id='dropdown-${phone.technicalId}' class='dropdown-content'>
                <li><a href="#!" onclick="editPhone('${phone.technicalId}')">Edytuj</a></li>
                <li>
                  <a href="#!"
                     class="${phone.status !== 'WPROWADZONY' ? 'disabled-link' : ''}"
                     onclick="${phone.status === 'WPROWADZONY' ? `acceptPhone('${phone.technicalId}')` : 'event.preventDefault()'}">
                     Przyjmij na sklep
                  </a>
                </li>
                <li class="red-text"><a href="#!" onclick="deletePhone('${phone.technicalId}')">Usuń</a></li>
              </ul>
            
              <button class="btn-small green darken-2 ${disableSellButton}" id=${sellButtonId}
                      onclick="sellPhone('${phone.technicalId}')">
                <i class="material-icons left">attach_money</i>Sprzedaj
              </button>
        </div>
        </div>
      </div>
    `;
        listContainer.innerHTML += card;
    });
}

let cartBtn = null;

// const cartBtn = document.getElementById('cartBtn'); // jeśli taki masz

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

    // zapisz ID w modalowym przycisku TAK
    const confirmBtn = document.getElementById("confirmAcceptYes");
    confirmBtn.setAttribute("data-technical-id", technicalId);

    // otwórz modal
    const modal = M.Modal.getInstance(document.getElementById("acceptConfirmModal"));
    modal.open();
}


async function sellPhone(technicalId) {
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

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }

        const updatedCart = await response.json();
        cartState = updatedCart;

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

    M.Modal.init(document.querySelectorAll('.modal'));

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

});

function editPhone(technicalId) {
    try {
        // znajdź kartę
        const card = document.querySelector(`.card[data-technical-id="${technicalId}"]`);
        if (!card) {
            console.error("Nie znaleziono karty telefonu dla ID:", technicalId);
            return;
        }

        // Pobierz dane z karty
        const name = card.querySelector(".card-title")?.textContent.trim() || "";
        const model = card.querySelector("p:nth-of-type(1)")?.textContent.replace("Model:", "").trim() || "";
        const color = card.querySelector("p:nth-of-type(2)")?.textContent.replace("Kolor:", "").trim() || "";
        const imei = card.querySelector("p:nth-of-type(3)")?.textContent.replace("IMEI:", "").trim() || "";
        const priceText = card.querySelector(".sellingPrice")?.textContent.replace("zł", "").trim() || "";
        const price = parseFloat(priceText) || 0;

        // Wypełnij formularz
        document.getElementById("editName").value = name;
        document.getElementById("editModel").value = model;
        document.getElementById("editColor").value = color;
        document.getElementById("editImei").value = imei;
        document.getElementById("editPrice").value = price;

        M.updateTextFields();

        // ustaw ID w przycisku zapisu
        const saveBtn = document.getElementById("saveEditBtn");
        saveBtn.setAttribute("data-technical-id", technicalId);

        // otwórz modal
        const modal = M.Modal.getInstance(document.getElementById("editPhoneModal"));
        modal.open();

    } catch (error) {
        console.error("Błąd podczas otwierania edycji:", error);
        M.toast({html: "Nie udało się otworzyć edycji telefonu", classes: "red"});
    }
}


document.getElementById("saveEditBtn").addEventListener("click", async () => {
    const technicalId = document
        .getElementById("saveEditBtn")
        .getAttribute("data-technical-id");

    const updatedPhone = {
        name: document.getElementById("editName").value,
        model: document.getElementById("editModel").value,
        color: document.getElementById("editColor").value,
        imei: document.getElementById("editImei").value,
        sellingPrice: parseFloat(document.getElementById("editPrice").value)
    };

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
        } else {
            M.toast({html: "Telefon został zaktualizowany", classes: "green"});
        }

        // Zamknij modal
        const modal = M.Modal.getInstance(document.getElementById("editPhoneModal"));
        modal.close();

        // Odśwież listę telefonów
        loadStock(currentPage);

    } catch (error) {
        console.error("Błąd podczas zapisywania zmian:", error);
        M.toast({html: "Błąd podczas zapisu", classes: "red"});
    }
});

document.getElementById("resetFilters").addEventListener("click", () => {
    document.getElementById("filterName").value = "";
    document.getElementById("filterModel").value = "";
    document.getElementById("filterColor").value = "";
    document.getElementById("filterImei").value = "";
    document.getElementById("filterPriceMin").value = "";
    document.getElementById("filterPriceMax").value = "";
    document.getElementById("filterStatus").value = "";
    document.getElementById("filterLocation").value = "";
    M.FormSelect.init(document.querySelectorAll("select"));


    if (M && M.FormSelect) {
        M.FormSelect.init(document.querySelectorAll('select'));
    }

    renderFilterChips();
    loadStock(0);
});

// === CHIPS SECTION === //
const chipContainer = document.getElementById("activeChips");

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


// generowanie chipów
function renderChips(filters) {
    chipContainer.innerHTML = "";

    Object.entries(filters).forEach(([key, val]) => {
        if (!val) return;

        const chip = document.createElement("div");
        chip.className = "chip";

        chip.innerHTML = `
            ${filterLabels[key]}: <b>${val}</b>
            <i class="close material-icons" data-filter="${key}">close</i>
        `;

        chipContainer.appendChild(chip);
    });

    // usuwanie chipa → czyści filtr → reload
    chipContainer.querySelectorAll(".close").forEach(icon => {
        icon.addEventListener("click", () => {
            const filterKey = icon.dataset.filter;
            document.getElementById("filter" + capitalize(filterKey)).value = "";

            if (filterKey === "status") {
                M.FormSelect.init(document.querySelectorAll('select'));
            }

            loadStock(0); // automatyczne odświeżenie
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

    Object.entries(filters).forEach(([key, value]) => {
        if (!value) return;

        const prettyName = {
            name: "Nazwa",
            model: "Model",
            color: "Kolor",
            imei: "IMEI",
            priceMin: "Cena od",
            priceMax: "Cena do",
            status: "Status"
        };

        const chip = document.createElement("div");
        chip.className = "chip";
        chip.innerHTML = `${prettyName[key]}: ${value} <i class="close material-icons">close</i>`;

        chip.querySelector("i").addEventListener("click", () => {
            document.getElementById("filter" + key.charAt(0).toUpperCase() + key.slice(1)).value = "";

            if (key === "status") {
                M.FormSelect.init(document.querySelectorAll("select"));
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

        locations.forEach(name => {
            const opt = document.createElement("option");
            opt.value = name;
            opt.textContent = name;
            select.appendChild(opt);
        });

        M.FormSelect.init(select);

    } catch (e) {
        console.warn("Nie udało się pobrać lokalizacji", e);
    }
}


function debounce(fn, delay = 300) {
    let timeout;
    return (...args) => {
        clearTimeout(timeout);
        timeout = setTimeout(() => fn(...args), delay);
    };
}

const liveReload = debounce(() => loadStock(0), 300);
