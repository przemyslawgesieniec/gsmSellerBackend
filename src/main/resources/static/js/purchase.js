const purchasePageSize = 20;
let purchaseCurrentPage = 1;
let currentPurchases = [];
let selectedPurchase = null;

const STATUS_LABELS = {
    NEW: "Nowe",
    OPEN: "Nowe",
    PRICE_AGREED: "Cena ustalona",
    PURCHASED: "Skup udany",
    CLOSED: "Zamknięte bez skupu"
};

const STATUS_BADGES = {
    NEW: "blue",
    OPEN: "blue",
    PRICE_AGREED: "amber darken-2",
    PURCHASED: "green darken-2",
    CLOSED: "grey darken-1"
};

document.addEventListener("DOMContentLoaded", () => {
    M.Modal.init(document.querySelectorAll(".modal"));
    M.FormSelect.init(document.querySelectorAll("select"));

    document.getElementById("refreshPurchasesBtn").addEventListener("click", () => loadPurchases(purchaseCurrentPage));
    document.getElementById("purchaseStatusFilter").addEventListener("change", () => loadPurchases(1));
    document.getElementById("purchaseSearch").addEventListener("input", debounce(() => loadPurchases(1), 300));
    document.getElementById("saveAgreedPriceBtn").addEventListener("click", saveAgreedPrice);
    document.getElementById("markPurchasedBtn").addEventListener("click", markPurchased);
    document.getElementById("closeFailedBtn").addEventListener("click", closePurchaseFailed);
    document.getElementById("addCommentBtn").addEventListener("click", addComment);

    loadPurchases(1);
});

async function loadPurchases(page = 1) {
    purchaseCurrentPage = page;
    const params = new URLSearchParams({
        page,
        size: purchasePageSize,
        statusGroup: document.getElementById("purchaseStatusFilter").value,
        search: document.getElementById("purchaseSearch").value.trim()
    });

    showPurchaseLoader(true);

    try {
        const response = await fetch(`/api/v1/purchase?${params.toString()}`);
        if (!response.ok) throw new Error("Błąd pobierania zgłoszeń");

        const data = await response.json();
        currentPurchases = data.content || [];
        renderPurchases(currentPurchases);
        renderPurchasePagination(data);
    } catch (error) {
        console.error("Error loading purchases:", error);
        M.toast({ html: "Błąd ładowania zgłoszeń skupu", classes: "red" });
    } finally {
        showPurchaseLoader(false);
    }
}

function renderPurchases(purchases) {
    const tbody = document.getElementById("purchaseTableBody");
    const empty = document.getElementById("purchaseEmptyState");
    tbody.innerHTML = "";
    empty.classList.toggle("hide", purchases.length > 0);

    purchases.forEach(purchase => {
        const lastComment = getLastComment(purchase);
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td>
                <strong>${formatDate(purchase.createdAt)}</strong>
                <br>
                <span class="grey-text">${formatTime(purchase.createdAt)}</span>
            </td>
            <td>
                <strong>${escapeHtml(purchase.phoneNumber || "")}</strong>
                ${purchase.contactedCustomer ? '<br><span class="grey-text">Kontakt wykonany</span>' : ""}
            </td>
            <td>
                <strong>${escapeHtml(purchase.phoneModel || "")}</strong>
                ${purchase.description ? `<br><span class="grey-text truncate">${escapeHtml(purchase.description)}</span>` : ""}
            </td>
            <td>${renderStatusBadge(purchase.status)}</td>
            <td>${purchase.agreedPrice ? `<strong>${formatMoney(purchase.agreedPrice)}</strong>` : '<span class="grey-text">—</span>'}</td>
            <td>
                <span class="grey-text">${purchase.photoIds?.length || 0} zdjęć</span>
                <br>
                <span class="grey-text">${purchase.comments?.length || 0} komentarzy</span>
            </td>
            <td>${lastComment ? renderCommentPreview(lastComment) : '<span class="grey-text">Brak komentarzy</span>'}</td>
            <td class="right-align">
                <button class="btn-small blue darken-2" data-action="details" data-id="${purchase.technicalId}" title="Szczegóły">
                    <i class="material-icons">visibility</i>
                </button>
            </td>
        `;

        tr.querySelector('[data-action="details"]').addEventListener("click", () => openDetails(purchase.technicalId));
        tbody.appendChild(tr);
    });
}

function openDetails(technicalId) {
    selectedPurchase = currentPurchases.find(item => item.technicalId === technicalId);
    if (!selectedPurchase) return;

    document.getElementById("detailPurchaseId").value = selectedPurchase.technicalId;
    document.getElementById("detailModel").textContent = selectedPurchase.phoneModel || "";
    document.getElementById("detailStatus").innerHTML = renderStatusBadge(selectedPurchase.status);
    document.getElementById("detailPhone").textContent = selectedPurchase.phoneNumber || "—";
    document.getElementById("detailDate").textContent = formatDateTime(selectedPurchase.createdAt);
    document.getElementById("detailPrice").textContent = selectedPurchase.agreedPrice ? formatMoney(selectedPurchase.agreedPrice) : "Nie ustalono";
    document.getElementById("detailContacted").textContent = selectedPurchase.contactedCustomer ? "Tak" : "Nie";
    document.getElementById("detailDescription").textContent = selectedPurchase.description || "Brak opisu";
    document.getElementById("agreedPriceInput").value = selectedPurchase.agreedPrice || "";
    document.getElementById("closureReasonInput").value = selectedPurchase.closureReason || "";
    document.getElementById("contactedCustomerInput").checked = selectedPurchase.contactedCustomer === true;
    document.getElementById("newCommentInput").value = "";

    renderPhotos(selectedPurchase.photoIds || []);
    renderComments(selectedPurchase.comments || []);
    updateActionAvailability(selectedPurchase.status);

    M.updateTextFields();
    M.textareaAutoResize(document.getElementById("closureReasonInput"));
    M.textareaAutoResize(document.getElementById("newCommentInput"));
    M.Modal.getInstance(document.getElementById("purchaseDetailsModal")).open();
}

function renderPhotos(photoIds) {
    const container = document.getElementById("detailPhotos");
    container.innerHTML = "";

    if (photoIds.length === 0) {
        container.innerHTML = '<p class="grey-text">Brak zdjęć</p>';
        return;
    }

    photoIds.forEach(id => {
        const link = document.createElement("a");
        link.href = `/api/v1/purchase/photos/${id}`;
        link.target = "_blank";
        link.rel = "noopener";
        link.innerHTML = `<img src="/api/v1/purchase/photos/${id}/thumbnail" alt="Zdjęcie skupu">`;
        container.appendChild(link);
    });
}

function renderComments(comments) {
    const container = document.getElementById("purchaseComments");
    container.innerHTML = "";

    if (comments.length === 0) {
        container.innerHTML = '<p class="grey-text">Brak komentarzy</p>';
        return;
    }

    comments.forEach(comment => {
        const item = document.createElement("div");
        item.className = "purchase-comment";
        item.innerHTML = `
            <div class="purchase-comment-meta">
                <strong>${escapeHtml(comment.authorUsername || "unknown")}</strong>
                <span>${formatDateTime(comment.createdAt)}</span>
            </div>
            <p>${escapeHtml(comment.content || "")}</p>
        `;
        container.appendChild(item);
    });
}

function updateActionAvailability(status) {
    const terminal = status === "PURCHASED" || status === "CLOSED";
    document.getElementById("purchaseActionSection").classList.toggle("hide", terminal);
}

async function saveAgreedPrice() {
    const id = document.getElementById("detailPurchaseId").value;
    const agreedPrice = readPriceInput();
    if (!agreedPrice) return;

    await postPurchaseAction(`/api/v1/purchase/${id}/price-agreed`, { agreedPrice }, "Cena została zapisana");
}

async function markPurchased() {
    const id = document.getElementById("detailPurchaseId").value;
    const agreedPrice = readPriceInput();
    if (!agreedPrice) return;

    await postPurchaseAction(`/api/v1/purchase/${id}/success`, { agreedPrice }, "Skup oznaczony jako udany");
}

async function closePurchaseFailed() {
    const id = document.getElementById("detailPurchaseId").value;
    const reason = document.getElementById("closureReasonInput").value.trim();

    if (!reason) {
        M.toast({ html: "Podaj powód zamknięcia", classes: "red" });
        document.getElementById("closureReasonInput").focus();
        return;
    }

    await postPurchaseAction(`/api/v1/purchase/${id}/close`, {
        reason,
        contactedCustomer: document.getElementById("contactedCustomerInput").checked
    }, "Zgłoszenie zamknięte bez skupu");
}

async function addComment() {
    const id = document.getElementById("detailPurchaseId").value;
    const content = document.getElementById("newCommentInput").value.trim();

    if (!content) {
        M.toast({ html: "Wpisz treść komentarza", classes: "red" });
        return;
    }

    try {
        const response = await fetch(`/api/v1/purchase/${id}/comments`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ content })
        });
        if (!response.ok) throw new Error("Błąd zapisu komentarza");

        M.toast({ html: "Komentarz dodany", classes: "green" });
        await reloadCurrentPageAndDetails(id);
    } catch (error) {
        console.error("Error adding purchase comment:", error);
        M.toast({ html: "Nie udało się dodać komentarza", classes: "red" });
    }
}

async function postPurchaseAction(url, payload, successMessage) {
    const id = document.getElementById("detailPurchaseId").value;

    try {
        const response = await fetch(url, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });
        if (!response.ok) {
            const message = await response.text();
            throw new Error(message || "Błąd zapisu");
        }

        M.toast({ html: successMessage, classes: "green" });
        await reloadCurrentPageAndDetails(id);
    } catch (error) {
        console.error("Error updating purchase:", error);
        M.toast({ html: "Nie udało się zapisać zmian", classes: "red" });
    }
}

async function reloadCurrentPageAndDetails(id) {
    await loadPurchases(purchaseCurrentPage);
    const refreshed = currentPurchases.find(item => item.technicalId === id);
    if (refreshed) {
        openDetails(id);
    } else {
        M.Modal.getInstance(document.getElementById("purchaseDetailsModal")).close();
    }
}

function readPriceInput() {
    const input = document.getElementById("agreedPriceInput");
    const value = input.value.trim();
    const parsed = Number.parseFloat(value);

    if (!value || Number.isNaN(parsed) || parsed <= 0) {
        M.toast({ html: "Podaj kwotę większą od 0", classes: "red" });
        input.focus();
        return null;
    }

    return value;
}

function renderPurchasePagination(data) {
    const pagination = document.getElementById("purchasePagination");
    pagination.innerHTML = "";

    if (!data || data.totalPages <= 1) return;

    const activePage = (data.number ?? 0) + 1;

    for (let page = 1; page <= data.totalPages; page++) {
        const li = document.createElement("li");
        li.className = page === activePage ? "active blue" : "waves-effect";
        li.innerHTML = `<a href="#!">${page}</a>`;
        li.addEventListener("click", () => loadPurchases(page));
        pagination.appendChild(li);
    }
}

function renderStatusBadge(status) {
    return `<span class="badge ${STATUS_BADGES[status] || "grey"} white-text purchase-status-badge">${escapeHtml(STATUS_LABELS[status] || status || "—")}</span>`;
}

function renderCommentPreview(comment) {
    return `
        <strong>${escapeHtml(comment.authorUsername || "unknown")}</strong>
        <br>
        <span class="grey-text">${formatDateTime(comment.createdAt)}</span>
        <br>
        <span>${escapeHtml(comment.content || "")}</span>
    `;
}

function getLastComment(purchase) {
    const comments = purchase.comments || [];
    return comments.length > 0 ? comments[comments.length - 1] : null;
}

function showPurchaseLoader(show) {
    document.getElementById("purchaseLoader").classList.toggle("hide", !show);
}

function formatMoney(value) {
    return new Intl.NumberFormat("pl-PL", {
        style: "currency",
        currency: "PLN"
    }).format(Number(value));
}

function formatDateTime(value) {
    if (!value) return "—";
    return new Date(value).toLocaleString("pl-PL");
}

function formatDate(value) {
    if (!value) return "—";
    return new Date(value).toLocaleDateString("pl-PL");
}

function formatTime(value) {
    if (!value) return "";
    return new Date(value).toLocaleTimeString("pl-PL", { hour: "2-digit", minute: "2-digit" });
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function debounce(fn, delay = 300) {
    let timeout;
    return (...args) => {
        clearTimeout(timeout);
        timeout = setTimeout(() => fn(...args), delay);
    };
}
