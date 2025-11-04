const pageSize = 5;
let currentPage = 0;

const backendPath = 'http://localhost:8090'
const listContainer = document.getElementById("phone-list");

async function loadStock(page = 0) {
    try {
        const data = await fetchPhonesPage(page);

        const phones = data.content.map(phone => ({
            technicalId: phone.technicalId,
            name: phone.name,
            model: phone.model,
            color: phone.color,
            imei: phone.imei1,
            sellingPrice: phone.sellingPrice,
            status: "DOSTĘPNY"
        }));

        renderPhones(phones);
        loadPagination(data.number, data.totalPages);
    } catch (error) {
        console.error('Błąd podczas pobierania danych:', error);
    }
}

async function fetchPhonesPage(page = 0) {
    try {
        const response = await fetch(backendPath + `/api/v1/phones?page=${page}&size=${pageSize}`);
        console.log("response is : " + response);
        if (!response.ok) {
            throw new Error(`HTTP error! Status: ${response.status}`);
        }
        const data = await response.json();
        console.log("Dane z backendu:", data);
        return data;
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
        const statusClass = phone.status === "DOSTĘPNY" ? "dostepny" : "sprzedany";
        const disabled = phone.status === "SPRZEDANY" ? "disabled" : "";
        const sellButtonId = sellButtonIdPrefix + phone.technicalId;
        const editButtonId = editButtonIdPrefix + phone.technicalId;

        const card = `
      <div class="col s12">
        <div class="card z-depth-2">
          <div class="card-content phone-card">
            <div class="phone-left">
              <span class="card-title">${phone.name}</span>
              <p><b>Model:</b> ${phone.model}</p>
              <p><b>Kolor:</b> ${phone.color}</p>
              <p><b>IMEI:</b> ${phone.imei}</p>
            </div>
            <div class="phone-right">
              <p class="sellingPrice">${phone.sellingPrice} zł</p>
              <span class="status-badge ${statusClass}">${phone.status}</span>
            </div>
          </div>
          <div class="card-action right-align">
            <button class="btn-small orange darken-2 id=${sellButtonId} ${disabled}" onclick="sellPhone('${phone.technicalId}')">
              <i class="material-icons left">attach_money</i>Sprzedaj
            </button>
            <button class="btn-small blue darken-2" id=${editButtonId} onclick="editPhone('${phone.technicalId}')">
              <i class="material-icons left">edit</i>Edytuj
            </button>
          </div>
        </div>
      </div>
    `;
        listContainer.innerHTML += card;
    });
}

let cartCount = 0;
let cartBtn = null;

function sellPhone(technicalId) {
    console.log("selling phone with id " + technicalId);
    const phone = phones[index];
    if (phone.status === "SPRZEDANY") return;

    phone.status = "SPRZEDANY";

    // Toast potwierdzający
    M.toast({html: `Telefon ${phone.name} dodany do koszyka`, classes: 'green'});

    // Zwiększ licznik
    cartCount += 1;
    cartBtn.querySelector('.badge').textContent = cartCount;
}

function createCartButton() {
    cartBtn = document.createElement('div');
    cartBtn.id = 'cartBtn';
    cartBtn.className = 'btn-floating btn-large blue';
    cartBtn.innerHTML = `<i class="material-icons">shopping_cart</i><span class="badge">0</span>`;
    document.body.appendChild(cartBtn);

    // Obsługa kliknięcia w koszyk
    cartBtn.addEventListener('click', () => {
        M.toast({html: `W koszyku ${cartCount} telefonów`, classes: 'blue'});
    });

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

    // Przycisk Dodaj w modal
    document.getElementById('addBulkBtn').addEventListener('click', () => {
        const price = document.getElementById('manualPrice').value;
        const source = document.getElementById('manualSource').value;
        const name = document.getElementById('manualName').value;

        console.log({price, source, name});
        M.toast({html: 'Telefony dodane (symulacja)', classes: 'blue'});
        const modal = M.Modal.getInstance(document.getElementById('addBulkModal'));
        modal.close();
    });

    createCartButton();
});

function editPhone(technicalId) {
    try {
        const editButton = document.getElementById(editButtonIdPrefix + technicalId);
        if (!editButton) {
            console.error("Nie znaleziono przycisku edycji dla ID:", technicalId);
            return;
        }

        // znajdź kartę (najbliższy .card)
        const card = editButton.closest(".card");
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

        // Wypełnij formularz w modalu
        document.getElementById("editName").value = name;
        document.getElementById("editModel").value = model;
        document.getElementById("editColor").value = color;
        document.getElementById("editImei").value = imei;
        document.getElementById("editPrice").value = price;

        // odśwież labelki Materialize
        M.updateTextFields();

        // zapisz ID telefonu w przycisku Zapisz
        const saveBtn = document.getElementById("saveEditBtn");
        saveBtn.setAttribute("data-technical-id", technicalId);

        // otwórz modal
        const modal = M.Modal.getInstance(document.getElementById("editPhoneModal"));
        modal.open();

    } catch (error) {
        console.error("Błąd podczas otwierania edycji:", error);
        M.toast({ html: "Nie udało się otworzyć edycji telefonu", classes: "red" });
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
        imei1: document.getElementById("editImei").value,
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

        if (!response.ok) throw new Error("Nie udało się zapisać zmian");

        M.toast({ html: "Telefon został zaktualizowany", classes: "green" });

        // Zamknij modal
        const modal = M.Modal.getInstance(document.getElementById("editPhoneModal"));
        modal.close();

        // Odśwież listę telefonów
        loadStock(currentPage);

    } catch (error) {
        console.error("Błąd podczas zapisywania zmian:", error);
        M.toast({ html: "Błąd podczas zapisu", classes: "red" });
    }
});

