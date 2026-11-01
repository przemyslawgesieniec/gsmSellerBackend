let CURRENT_USER = null;
let IS_ADMIN = false;
let currentUserRequest = null;
async function loadCurrentUser() {
    if (!currentUserRequest) currentUserRequest = fetch('/api/v1/users/auth/me').then(async response => {
        if (!response.ok || response.redirected) return null;
        return response.json();
    }).catch(() => null);
    CURRENT_USER = await currentUserRequest;
    IS_ADMIN = !!(CURRENT_USER?.roles?.includes('ROLE_ADMIN') || CURRENT_USER?.role === 'ROLE_ADMIN');
    document.querySelectorAll('.admin-only').forEach(el => el.classList.toggle('hide', !IS_ADMIN));
    return CURRENT_USER;
}
