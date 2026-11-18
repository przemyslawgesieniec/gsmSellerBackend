function configureRepairDeletion(repair, onDeleted) {
    const button = document.getElementById('deleteRepairBtn');
    const canDelete = Boolean(IS_ADMIN && repair?.businessId);
    button.hidden = !canDelete;
    button.style.display = canDelete ? 'inline-block' : 'none';
    button.onclick = canDelete ? () => openRepairDeletion(repair, onDeleted) : null;
}

function openRepairDeletion(repair, onDeleted) {
    const dialog = document.getElementById('deleteRepairDialog');
    const form = document.getElementById('deleteRepairForm');
    const input = document.getElementById('deleteRepairRma');
    const confirm = document.getElementById('confirmDeleteRepairBtn');
    const cancel = document.getElementById('cancelDeleteRepairBtn');
    const error = document.getElementById('deleteRepairError');
    let deleting = false;

    form.reset();
    input.disabled = false;
    confirm.disabled = true;
    cancel.disabled = false;
    error.hidden = true;
    document.getElementById('deleteRepairExpectedRma').textContent = repair.businessId;
    input.oninput = () => { confirm.disabled = input.value !== repair.businessId; };
    input.onpaste = input.ondrop = event => event.preventDefault();
    cancel.onclick = () => dialog.close();
    dialog.onkeydown = event => {
        if (event.key === 'Escape') event.stopPropagation();
    };
    dialog.oncancel = event => { if (deleting) event.preventDefault(); };
    const preventBackdropClose = event => {
        if (deleting && event.target === dialog) event.stopImmediatePropagation();
    };
    dialog.addEventListener('click', preventBackdropClose, true);
    dialog.addEventListener('close', () => dialog.removeEventListener('click', preventBackdropClose, true), { once: true });

    form.onsubmit = async event => {
        event.preventDefault();
        if (deleting || input.value !== repair.businessId) return;
        deleting = true;
        input.disabled = confirm.disabled = cancel.disabled = true;
        error.hidden = true;

        try {
            const response = await fetch(`/api/v1/repairs/${encodeURIComponent(repair.technicalId)}`, {
                method: 'DELETE',
                headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
                body: JSON.stringify({ rma: input.value })
            });
            if (response.status !== 204) {
                error.textContent = {
                    400: 'Numer RMA jest nieprawidłowy.',
                    403: 'Tylko administrator może usunąć naprawę.',
                    404: 'Naprawa już nie istnieje.'
                }[response.status] || 'Nie udało się usunąć naprawy. Odśwież widok i sprawdź, czy nadal jest na liście.';
                error.hidden = false;
                return;
            }
            dialog.close();
            onDeleted();
        } catch {
            error.textContent = 'Nie udało się potwierdzić usunięcia. Odśwież widok i sprawdź, czy naprawa jest na liście.';
            error.hidden = false;
        } finally {
            deleting = false;
            input.disabled = cancel.disabled = false;
            confirm.disabled = input.value !== repair.businessId;
        }
    };

    dialog.showModal();
}
