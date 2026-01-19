let CURRENT_USER = null;
let IS_ADMIN = false;

document.addEventListener("DOMContentLoaded", async () => {
    await loadCurrentUser();

    // ⬅️ widoczność sekcji lokalizacji MUSI być po loadCurrentUser

    if (canUseLocationSelect()) {
        handleLocationSettingsVisibility();
        initLocationSelect();
        loadLocations();

        document.getElementById("currentLocationLabel").textContent =
            CURRENT_USER.location ?? "Nie masz przypisanej lokalizacji";
    }
});

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

            locations.forEach(loc => {
                ts.addOption({
                    value: loc.technicalId,
                    text: `${loc.name}${loc.city ? " – " + loc.city : ""}`
                });
            });

            ts.refreshOptions(false);

            if (CURRENT_USER?.location) {
                ts.setValue(CURRENT_USER.location);
            } else {
                ts.clear();
            }

            ts.off("change");
            ts.on("change", value => {
                if (value) assignLocation(value);
            });
        });
}

function assignLocation(technicalId) {
    fetch(`/api/v1/users/location/${technicalId}`, {
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

        const isActive = u.status === 'ACTIVE';
        const isAdmin = u.role === 'ROLE_ADMIN';
        const isDynamic = u.role === 'ROLE_DYNAMIC_LOCATION_SELLER';

        tr.innerHTML = `
          <td><b>${u.username}</b></td>

          <td>${u.location ?? '<span class="grey-text">—</span>'}</td>

          <td>
            <div class="switch">
              <label>
                Off
               <input type="checkbox"
                   ${isDynamic ? 'checked' : ''}
                   ${isAdmin ? 'disabled' : ''}
                   onchange="toggleDynamicLocation(this, ${u.id})">
                <span class="lever"></span>
                On
              </label>
            </div>
          </td>

       <td>
          <div class="switch">
            <label>
              Statyczna
              <input type="checkbox"
                     ${isDynamic ? 'checked' : ''}
                     ${isAdmin ? 'disabled' : ''}
                     onchange="toggleDynamicLocation(this, ${u.id})">
              <span class="lever"></span>
              Dynamiczna
            </label>
          </div>
        </td>

        `;

        tbody.appendChild(tr);
    });
}

async function toggleUserStatus(userId) {
    if (!IS_ADMIN) return;

    const res = await fetch(`/api/v1/admin/users/${userId}/toggle-status`, {
        method: 'PUT'
    });

    if (!res.ok) {
        M.toast({ html: 'Nie można zmienić statusu', classes: 'red' });
        return;
    }

    M.toast({ html: 'Status użytkownika zmieniony', classes: 'green' });
    loadAllUsers();
}

async function toggleDynamicLocation(checkbox, userId) {
    if (checkbox.disabled) return;

    const previousState = checkbox.checked;

    const res = await fetch(
        `/api/v1/admin/users/${userId}/toggle-dynamic-location`,
        { method: 'PUT' }
    );

    if (!res.ok) {
        checkbox.checked = !previousState;
        M.toast({
            html: 'Nie udało się zmienić trybu lokalizacji',
            classes: 'red'
        });
        return;
    }

    M.toast({
        html: 'Tryb lokalizacji zmieniony',
        classes: 'green'
    });
}

/* =======================
   CURRENT USER
   ======================= */

async function loadCurrentUser() {
    const res = await fetch('/api/v1/users/auth/me');
    if (!res.ok) return;

    CURRENT_USER = await res.json();
    IS_ADMIN = CURRENT_USER.role === 'ROLE_ADMIN';

    if (IS_ADMIN) {
        document
            .querySelectorAll('.admin-only')
            .forEach(el => el.classList.remove('hide'));

        await loadAllUsers();
    }
}
