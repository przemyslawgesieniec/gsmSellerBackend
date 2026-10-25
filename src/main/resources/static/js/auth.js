// auth.js
let CURRENT_USER = null;
let IS_ADMIN = false;

async function loadCurrentUser() {
    try {
        if (!CURRENT_USER) {
            const res = await fetch('/api/v1/users/auth/me');
            if (!res.ok) return null;
            CURRENT_USER = await res.json();
        }

        IS_ADMIN =
            CURRENT_USER.roles?.includes('ROLE_ADMIN') ||
            CURRENT_USER.role === 'ROLE_ADMIN';

        // pokaż elementy admin-only (navbar, inne widoki)
        if (IS_ADMIN) {
            document
                .querySelectorAll('.admin-only')
                .forEach(el => el.classList.remove('hide'));
        }

        return CURRENT_USER;

    } catch (e) {
        console.warn("Nie udało się pobrać użytkownika", e);
        return null;
    }
}
