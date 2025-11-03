const pageSize = 5;
let currentPage = 0;

const backendPath = 'http://localhost:8090'
const listContainer = document.getElementById("phone-list");

// let phones = []; // zamiast zahardkodowanych danych

async function loadStock(page = 0) {
    try {
        const data = await fetchPhonesPage(page);

        const phones = data.content.map(phone => ({
            name: phone.name,
            model: phone.model,
            color: phone.color,
            imei: phone.imei1,
            price: phone.suggestedSellingPrice,
            status: "DOSTĘPNY"})); // możesz ustawić domyślnie, dopóki backend tego nie zwraca

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
        M.toast({ html: 'Błąd ładowania danych z serwera', classes: 'red' });
    }
}
function loadPagination(currentPage, totalPages) {
    const pagination = document.getElementById("dynamicPagination");
    if (!pagination) return;
    pagination.innerHTML = "";

    function createPageItem({ classes = "", inner = "", onClick = null }) {
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
function renderPhones(phones) {
    listContainer.innerHTML = "";
    phones.forEach((phone, index) => {
        const statusClass = phone.status === "DOSTĘPNY" ? "dostepny" : "sprzedany";
        const disabled = phone.status === "SPRZEDANY" ? "disabled" : "";

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
              <p class="price">${phone.price} zł</p>
              <span class="status-badge ${statusClass}">${phone.status}</span>
            </div>
          </div>
          <div class="card-action right-align">
            <button class="btn-small orange darken-2 ${disabled}" onclick="sellPhone(${index})">
              <i class="material-icons left">attach_money</i>Sprzedaj
            </button>
            <button class="btn-small blue darken-2" onclick="editPhone(${index})">
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

function sellPhone(index) {
    const phone = phones[index];
    if (phone.status === "SPRZEDANY") return;

    phone.status = "SPRZEDANY";
    // renderPhones();

    // Toast potwierdzający
    M.toast({ html: `Telefon ${phone.name} dodany do koszyka`, classes: 'green' });

    // Jeśli koszyk nie istnieje, stwórz go
    if (!cartBtn) {
        cartBtn = document.createElement('div');
        cartBtn.id = 'cartBtn';
        cartBtn.className = 'btn-floating btn-large blue';
        cartBtn.innerHTML = `<i class="material-icons">shopping_cart</i><span class="badge">0</span>`;
        document.body.appendChild(cartBtn);

        // Obsługa kliknięcia w koszyk
        cartBtn.addEventListener('click', () => {
            M.toast({ html: `W koszyku ${cartCount} telefonów`, classes: 'blue' });
        });
    }

    // Zwiększ licznik
    cartCount += 1;
    cartBtn.querySelector('.badge').textContent = cartCount;
}


// Inicjalizacja po załadowaniu DOM
// document.addEventListener("DOMContentLoaded", renderPhones);

document.addEventListener("DOMContentLoaded", function() {
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
        M.toast({ html: `${files.length} plików wybranych`, classes: 'green' });
        // Tutaj później będzie logika przesyłania do backendu
    }

    // Przycisk Dodaj w modal
    document.getElementById('addBulkBtn').addEventListener('click', () => {
        const price = document.getElementById('manualPrice').value;
        const source = document.getElementById('manualSource').value;
        const name = document.getElementById('manualName').value;

        console.log({ price, source, name });
        M.toast({ html: 'Telefony dodane (symulacja)', classes: 'blue' });
        const modal = M.Modal.getInstance(document.getElementById('addBulkModal'));
        modal.close();
    });
});

let currentEditIndex = null; // indeks edytowanego telefonu

function editPhone(index) {
    currentEditIndex = index;
    const phone = phones[index];

    // Wypełnij pola modal
    document.getElementById('editName').value = phone.name;
    document.getElementById('editModel').value = phone.model;
    document.getElementById('editColor').value = phone.color;
    document.getElementById('editImei').value = phone.imei;
    document.getElementById('editPrice').value = phone.price;

    // Odśwież Materialize labelki
    M.updateTextFields();

    // Otwórz modal
    const modal = M.Modal.getInstance(document.getElementById('editPhoneModal'));
    modal.open();
}

// Zapisz zmiany
document.getElementById('saveEditBtn').addEventListener('click', () => {
    if (currentEditIndex === null) return;

    const phone = phones[currentEditIndex];
    phone.name = document.getElementById('editName').value;
    phone.model = document.getElementById('editModel').value;
    phone.color = document.getElementById('editColor').value;
    phone.imei = document.getElementById('editImei').value;
    phone.price = parseFloat(document.getElementById('editPrice').value);

    // renderPhones(); // odśwież kafle
    M.toast({ html: `Telefon ${phone.name} zaktualizowany`, classes: 'green' });

    const modal = M.Modal.getInstance(document.getElementById('editPhoneModal'));
    modal.close();
});

