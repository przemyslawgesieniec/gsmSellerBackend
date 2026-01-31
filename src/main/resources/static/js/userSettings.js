document.addEventListener("DOMContentLoaded", async () => {
    await loadCurrentUser(); // 🔥 z auth.js

    // CARD zawsze widoczny
    showLocationCard();

    // aktualna lokalizacja
    setCurrentLocationLabel();

    // select lokalizacji
    if (canUseLocationSelect()) {
        showLocationSelect();
        initLocationSelect();
        loadLocations();
    }

    // ADMIN
    if (IS_ADMIN) {
        await loadAllUsers();
    }
});


function showLocationCard() {
    document
        .querySelectorAll('.location-settings')
        .forEach(el => el.classList.remove('hide'));
}

function setCurrentLocationLabel() {
    const label = document.getElementById("currentLocationLabel");
    if (!label) return;

    label.textContent =
        CURRENT_USER?.location ?? "Brak lokalizacji";
}

function showLocationSelect() {
    document
        .querySelectorAll('.location-select-wrapper')
        .forEach(el => el.classList.remove('hide'));
}

/* =======================
   USER / ROLE LOGIC
   ======================= */

function canUseLocationSelect() {
    return (
        CURRENT_USER.role === 'ROLE_ADMIN' ||
        CURRENT_USER.role === 'ROLE_DYNAMIC_LOCATION_SELLER'
    );
}

function handleLocationSettingsVisibility() {
        document
            .querySelectorAll('.location-settings')
            .forEach(el => el.classList.remove('hide'));
}

/* =======================
   LOCATION
   ======================= */

function initLocationSelect() {
    const select = document.getElementById('locationSelect');
    if (!select) return;

    new TomSelect(select, {
        create: false,
        shouldSort: false,
        placeholder: "Wybierz inną lokalizację"
    });
}

function loadLocations() {
    fetch("/api/v1/locations")
        .then(res => res.json())
        .then(locations => {
            const select = document.getElementById("locationSelect");
            if (!select || !select.tomselect) return;

            const ts = select.tomselect;

            ts.clear();
            ts.clearOptions();

            ts.addOption({
                value: "",
                text: "Brak lokalizacji"
            });

            locations.forEach(loc => {
                ts.addOption({
                    value: loc.technicalId,
                    text: `${loc.name}${loc.city ? " – " + loc.city : ""}`
                });
            });

            ts.refreshOptions(false);

            if (CURRENT_USER?.location) {
                // Znajdź technicalId dla aktualnej nazwy lokalizacji, aby ustawić wartość w select
                const currentLoc = locations.find(l => l.name === CURRENT_USER.location);
                if (currentLoc) {
                    ts.setValue(currentLoc.technicalId);
                } else {
                    ts.setValue("");
                }
            } else {
                ts.setValue("");
            }

            ts.off("change");
            ts.on("change", value => {
                assignLocation(value);
            });
        });
}

function assignLocation(technicalId) {
    const url = technicalId ? `/api/v1/users/location/${technicalId}` : `/api/v1/users/location`;
    fetch(url, {
        method: "PUT"
    })
        .then(res => {
            if (!res.ok) throw new Error();
            M.toast({ html: "Lokalizacja została zapisana", classes: "green" });
        })
        .catch(() => {
            M.toast({ html: "Nie udało się zapisać lokalizacji", classes: "red" });
        });
}

/* =======================
   ADMIN
   ======================= */

async function loadAllUsers() {
    if (!IS_ADMIN) return;

    const res = await fetch('/api/v1/admin/users');
    if (!res.ok) return;

    const users = await res.json();
    users.sort((a, b) => a.id - b.id);
    const tbody = document.getElementById('usersTableBody');
    if (!tbody) return;

    tbody.innerHTML = '';

    users.forEach(u => {
        const tr = document.createElement('tr');
        //
        // const isActive = u.status === 'ACTIVE';
        // const isAdmin = u.role === 'ROLE_ADMIN';
        // const isDynamic = u.role === 'ROLE_DYNAMIC_LOCATION_SELLER';

        tr.innerHTML = `
  <td><b>${u.username}</b></td>

  <td>${u.location ?? '<span class="grey-text">Brak lokalizacji</span>'}</td>

  <td>
    <div class="switch">
      <label>
        Nieaktywny
        <input type="checkbox"
               ${u.status === 'ACTIVE' ? 'checked' : ''}
               ${u.role === 'ROLE_ADMIN' ? 'disabled' : ''}
               onchange="setUserActive(this, ${u.id})">
        <span class="lever"></span>
        Aktywny
      </label>
    </div>
  </td>

  <td>
    <div class="switch">
      <label>
        Statyczna
        <input type="checkbox"
               ${u.role === 'ROLE_DYNAMIC_LOCATION_SELLER' ? 'checked' : ''}
               ${u.role === 'ROLE_ADMIN' ? 'disabled' : ''}
               onchange="setDynamicLocation(this, ${u.id})">
        <span class="lever"></span>
        Dynamiczna
      </label>
    </div>
  </td>
`;

        tbody.appendChild(tr);
    });
}

/* =======================
   CURRENT USER
   ======================= */

async function setUserActive(checkbox, userId) {
    const active = checkbox.checked;

    const res = await fetch(`/api/v1/admin/users/${userId}/active`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ active })
    });

    if (!res.ok) {
        checkbox.checked = !active;
        M.toast({
            html: 'Nie udało się zmienić statusu użytkownika',
            classes: 'red'
        });
        return;
    }

    M.toast({
        html: 'Status użytkownika zapisany',
        classes: 'green'
    });
}
async function setDynamicLocation(checkbox, userId) {
    const enabled = checkbox.checked;

    const res = await fetch(`/api/v1/admin/users/${userId}/dynamic-location`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ enabled })
    });

    if (!res.ok) {
        checkbox.checked = !enabled;
        M.toast({
            html: 'Nie udało się zmienić trybu lokalizacji',
            classes: 'red'
        });
        return;
    }

    M.toast({
        html: 'Tryb lokalizacji zapisany',
        classes: 'green'
    });
}
