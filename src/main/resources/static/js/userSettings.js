let CURRENT_USER = null;
let IS_ADMIN = false;


document.addEventListener("DOMContentLoaded", async () => {
    await loadCurrentUser();

    initLocationSelect();
    loadLocations();

    document.getElementById("currentLocationLabel").textContent =
        CURRENT_USER.location ?? "Nie masz przypisanej lokalizacji";
});


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
                ts.clear(); // ⬅️ to zostawia pole puste, ale placeholder działa
            }

            ts.off("change");
            ts.on("change", value => {
                if (value) {
                    assignLocation(value);
                }
            });
        });
}

function assignLocation(technicalId) {
    fetch(`/api/v1/users/location/${technicalId}`, {
        method: "PUT"
    })
        .then(res => {
            if (!res.ok) throw new Error();
            M.toast({
                html: "Lokalizacja została zapisana",
                classes: "green"
            });
        })
        .catch(() => {
            M.toast({
                html: "Nie udało się zapisać lokalizacji",
                classes: "red"
            });
        });
}
function initLocationSelect() {
    const select = document.getElementById('locationSelect');
    if (!select) return;

    new TomSelect(select, {
        create: false,
        shouldSort: false,
        placeholder: "Wybierz inną lokalizację"
    });
}


async function loadAllUsers() {
    if (!IS_ADMIN) return;

    const res = await fetch('/api/v1/admin/users');
    if (!res.ok) return;

    const users = await res.json();
    const list = document.getElementById('usersList');
    if (!list) return;

    list.innerHTML = '';

    users.forEach(u => {
        const li = document.createElement('li');
        li.className = 'collection-item';

        const isActive = u.status === 'ACTIVE';
        const isAdmin = u.role === 'ROLE_ADMIN';

        const disableDeactivate = isAdmin && isActive;

        li.innerHTML = `
          <div class="user-email">${u.username}</div>

          <div class="user-actions">
            <button
              class="btn-small ${isActive ? 'red' : 'green'}"
              ${disableDeactivate ? 'disabled' : ''}
              onclick="toggleUserStatus(${u.id})"
              title="${disableDeactivate ? 'Nie można dezaktywować administratora' : ''}"
            >
              ${isActive ? 'DEZAKTYWUJ' : 'AKTYWUJ'}
            </button>

            <span class="status-badge ${isActive ? 'status-active' : 'status-inactive'}">
              ${u.status}
            </span>
          </div>
        `;

        list.appendChild(li);
    });
}
async function toggleUserStatus(userId) {
    if (!IS_ADMIN) return;

    const res = await fetch(`/api/v1/admin/users/${userId}/toggle-status`, {
        method: 'PUT'
    });

    if (!res.ok) {
        const msg = await res.text();
        M.toast({
            html: msg || 'Nie można zmienić statusu użytkownika',
            classes: 'red'
        });
        return;
    }

    M.toast({ html: 'Status użytkownika zmieniony', classes: 'green' });
    loadAllUsers();
}

async function loadCurrentUser() {
    const res = await fetch('/api/v1/users/auth/me');
    if (!res.ok) return;

    CURRENT_USER = await res.json();
    IS_ADMIN = CURRENT_USER.role === 'ROLE_ADMIN';

    console.log("CURRENT_USER", CURRENT_USER); // ⬅️ MUSI BYĆ location

    if (IS_ADMIN) {
        document
            .querySelectorAll('.admin-only')
            .forEach(el => el.classList.remove('hide'));

        await loadAllUsers();
    }
}
