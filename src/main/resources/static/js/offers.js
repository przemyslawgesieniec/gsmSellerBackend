let photos = [];
let phoneSelect;
let offerLocationSelect;
let currentPage = 1;
let offerFiltersDebounce;
let isRestoringOfferListState = false;

document.addEventListener("DOMContentLoaded", async function() {
    initOfferFormToggle();
    initPhoneSelect();
    initOfferFilters();

    document.getElementById("photoFiles").addEventListener("change", handleFileSelect);
    document.getElementById("offerForm").addEventListener("submit", saveOffer);
    document.getElementById("clearPhone").addEventListener("click", clearSelectedPhone);
    document.getElementById("cancelBtn").addEventListener("click", resetForm);

    await loadOfferLocations();
    loadOffers(restoreOfferListStateFromUrl());
});

function initOfferFormToggle() {
    const toggle = document.getElementById("offerFormToggle");
    if (!toggle) return;

    toggle.addEventListener("click", () => toggleOfferForm());
    toggle.addEventListener("keydown", event => {
        if (event.key === "Enter" || event.key === " ") {
            event.preventDefault();
            toggleOfferForm();
        }
    });
}

function toggleOfferForm(forceOpen) {
    const body = document.getElementById("offerFormBody");
    const toggle = document.getElementById("offerFormToggle");
    const icon = document.getElementById("offerFormToggleIcon");
    if (!body) return;

    const shouldOpen = typeof forceOpen === "boolean" ? forceOpen : body.classList.contains("hide");
    body.classList.toggle("hide", !shouldOpen);
    if (toggle) toggle.setAttribute("aria-expanded", String(shouldOpen));
    if (icon) icon.textContent = shouldOpen ? "expand_less" : "expand_more";
}

function initPhoneSelect() {
    phoneSelect = new TomSelect("#phoneSelect", {
        valueField: "technicalId",
        labelField: "displayText",
        searchField: ["name", "model", "imei", "phoneModelDisplayName"],
        preload: true,
        load: function(query, callback) {
            fetch(`/api/v1/offers/available-phones?search=${encodeURIComponent(query || "")}&size=10`)
                .then(response => response.json())
                .then(json => {
                    callback((json.content || []).map(p => ({
                        ...p,
                        displayText: `${p.name || ""} ${p.model || ""} (${p.phoneModelDisplayName || "bez modelu z bazy"})`
                    })));
                })
                .catch(() => callback());
        },
        onChange: function(value) {
            if (value) {
                selectPhone(phoneSelect.options[value]);
            }
        },
        render: {
            option: function(item, escape) {
                return `<div>
                    <span class="title"><b>${escape(item.name || "")}</b> ${escape(item.model || "")}</span><br>
                    <small class="grey-text">
                        ${escape(item.phoneModelDisplayName || "")} | IMEI: ${escape(item.imei || "")} | Cena: ${escape(item.sellingPrice || "")} PLN
                    </small>
                </div>`;
            }
        }
    });
}

function initOfferFilters() {
    const imeiInput = document.getElementById("offerImeiFilter");
    const nameInput = document.getElementById("offerNameFilter");
    const clearButton = document.getElementById("clearOfferFilters");

    [imeiInput, nameInput].forEach(input => {
        input?.addEventListener("input", () => {
            clearTimeout(offerFiltersDebounce);
            currentPage = 1;
            syncOfferListStateToUrl(1);
            offerFiltersDebounce = setTimeout(() => loadOffers(1), 250);
        });
    });

    offerLocationSelect = new TomSelect("#offerLocationFilter", {
        create: false,
        allowEmptyOption: true,
        onChange: () => {
            if (!isRestoringOfferListState) {
                loadOffers(1);
            }
        }
    });

    clearButton?.addEventListener("click", () => {
        if (imeiInput) imeiInput.value = "";
        if (nameInput) nameInput.value = "";
        M.updateTextFields();
        offerLocationSelect?.clear(true);
        loadOffers(1);
    });
}

function restoreOfferListStateFromUrl() {
    isRestoringOfferListState = true;
    try {
        const params = new URLSearchParams(window.location.search);
        const imeiInput = document.getElementById("offerImeiFilter");
        const nameInput = document.getElementById("offerNameFilter");
        const location = params.get("location") || "";

        if (imeiInput) imeiInput.value = params.get("imei") || "";
        if (nameInput) nameInput.value = params.get("name") || "";

        if (offerLocationSelect) {
            if (location && !offerLocationSelect.options[location]) {
                offerLocationSelect.addOption({ value: location, text: location });
            }
            offerLocationSelect.setValue(location, true);
        }

        M.updateTextFields();
        return normalizeOfferPage(params.get("page"));
    } finally {
        isRestoringOfferListState = false;
    }
}

function normalizeOfferPage(page) {
    const parsed = Number.parseInt(page, 10);
    return Number.isInteger(parsed) && parsed > 0 ? parsed : 1;
}

function syncOfferListStateToUrl(page = currentPage) {
    const url = new URL(window.location.href);
    const imei = document.getElementById("offerImeiFilter")?.value.trim();
    const name = document.getElementById("offerNameFilter")?.value.trim();
    const location = offerLocationSelect?.getValue();

    url.searchParams.set("page", String(normalizeOfferPage(page)));
    setOrDeleteSearchParam(url.searchParams, "imei", imei);
    setOrDeleteSearchParam(url.searchParams, "name", name);
    setOrDeleteSearchParam(url.searchParams, "location", location);

    window.history.replaceState({}, "", url);
}

function setOrDeleteSearchParam(params, key, value) {
    if (value) {
        params.set(key, value);
    } else {
        params.delete(key);
    }
}

async function loadOfferLocations() {
    try {
        const response = await fetch("/api/v1/locations");
        if (!response.ok) throw new Error("Nie udało się pobrać lokalizacji");
        const locations = await response.json();

        if (!offerLocationSelect) return;
        offerLocationSelect.addOption({ value: "__ONLINE__", text: "Dostępny online" });
        locations.forEach(location => {
            offerLocationSelect.addOption({
                value: location.name,
                text: [location.name, location.city].filter(Boolean).join(" - ")
            });
        });
        offerLocationSelect.refreshOptions(false);
    } catch (err) {
        console.warn("Nie udało się pobrać lokalizacji ofert", err);
    }
}

function selectPhone(phone) {
    document.getElementById("phoneSelectWrapper").classList.add("hide");
    document.getElementById("selectedPhoneInfo").classList.remove("hide");
    document.getElementById("phoneDetails").textContent =
        `${phone.name || ""} ${phone.model || ""} | ${phone.phoneModelDisplayName || "model z bazy"}`;
    document.getElementById("editTechnicalId").value = phone.technicalId;
    renderSelectedPhonePreview(phone);
}

function renderSelectedPhonePreview(phone) {
    const preview = document.getElementById("modelSpecsPreview");
    preview.classList.remove("hide");
    preview.innerHTML = `
        <div class="card-panel grey lighten-5 model-specs-panel">
            <div><b>Telefon:</b> ${escapeHtml(phone.name || "")} ${escapeHtml(phone.model || "")}</div>
            <div><b>Model z bazy:</b> ${escapeHtml(phone.phoneModelDisplayName || "Nie ustawiono")}</div>
            <div><b>IMEI:</b> ${escapeHtml(phone.imei || "brak")}</div>
            <div><b>Pamięć:</b> ${escapeHtml([phone.memory, phone.ram].filter(Boolean).join(" / ") || "z bazy modelu")}</div>
            <div><b>Cena:</b> ${escapeHtml(phone.sellingPrice || "")} PLN</div>
        </div>
    `;
}

function clearSelectedPhone() {
    document.getElementById("phoneSelectWrapper").classList.remove("hide");
    document.getElementById("selectedPhoneInfo").classList.add("hide");
    document.getElementById("editTechnicalId").value = "";
    document.getElementById("modelSpecsPreview").classList.add("hide");
    document.getElementById("modelSpecsPreview").innerHTML = "";
    phoneSelect.clear();
    phoneSelect.clearOptions();
}

function handleFileSelect(e) {
    const files = Array.from(e.target.files);
    if (photos.length + files.length > 5) {
        M.toast({ html: "Można dodać maksymalnie 5 zdjęć", classes: "red" });
        e.target.value = "";
        return;
    }

    files.forEach(file => photos.push(file));
    renderPhotosPreview();
    e.target.value = "";
}

function renderPhotosPreview() {
    const container = document.getElementById("photosPreview");
    container.innerHTML = "";

    photos.forEach((photo, index) => {
        const div = document.createElement("div");
        div.className = "photo-container";

        const src = photo instanceof File ? getFilePreviewUrl(photo) : "https://via.placeholder.com/120x100?text=...";
        const dataSrc = photo instanceof File ? "" : photo.thumbnailUrl;

        div.innerHTML = `
            <span class="photo-order-badge">${index + 1}</span>
            <img src="${src}" ${dataSrc ? `data-src="${escapeHtml(dataSrc)}"` : ""} alt="Foto ${index + 1}">
            <i class="material-icons remove-photo" onclick="removePhoto(${index})">close</i>
            <div class="photo-primary-label">${index === 0 ? "Miniaturka" : "&nbsp;"}</div>
            <div class="photo-order-actions">
                <button type="button" class="btn-flat waves-effect" title="Przesuń wcześniej" onclick="movePhoto(${index}, -1)" ${index === 0 ? "disabled" : ""}>
                    <i class="material-icons">arrow_back</i>
                </button>
                <button type="button" class="btn-flat waves-effect" title="Ustaw jako miniaturkę" onclick="setPrimaryPhoto(${index})" ${index === 0 ? "disabled" : ""}>
                    <i class="material-icons">star</i>
                </button>
                <button type="button" class="btn-flat waves-effect" title="Przesuń później" onclick="movePhoto(${index}, 1)" ${index === photos.length - 1 ? "disabled" : ""}>
                    <i class="material-icons">arrow_forward</i>
                </button>
            </div>
        `;
        container.appendChild(div);
    });

    if (photos.some(p => !(p instanceof File))) {
        setTimeout(lazyLoadImages, 50);
    }
}

function getFilePreviewUrl(file) {
    if (!file.previewUrl) {
        file.previewUrl = URL.createObjectURL(file);
    }
    return file.previewUrl;
}

function removePhoto(index) {
    const photo = photos[index];
    revokePreviewUrl(photo);
    photos.splice(index, 1);
    renderPhotosPreview();
}

function movePhoto(index, direction) {
    const nextIndex = index + direction;
    if (nextIndex < 0 || nextIndex >= photos.length) return;
    const [photo] = photos.splice(index, 1);
    photos.splice(nextIndex, 0, photo);
    renderPhotosPreview();
}

function setPrimaryPhoto(index) {
    if (index <= 0 || index >= photos.length) return;
    const [photo] = photos.splice(index, 1);
    photos.unshift(photo);
    renderPhotosPreview();
}

async function saveOffer(e) {
    e.preventDefault();

    const technicalId = document.getElementById("editTechnicalId").value;
    if (!technicalId) {
        M.toast({ html: "Wybierz telefon!", classes: "red" });
        return;
    }

    const saveBtn = document.getElementById("saveBtn");
    const formLoader = document.getElementById("formLoader");
    const cancelBtn = document.getElementById("cancelBtn");
    const isEdit = cancelBtn && !cancelBtn.classList.contains("hide");

    if (saveBtn) saveBtn.disabled = true;
    if (formLoader) formLoader.classList.remove("hide");

    const formData = new FormData();
    if (!isEdit) {
        formData.append("phoneStockTechnicalId", technicalId);
    }

    const newPhotos = photos.filter(photo => photo instanceof File);
    newPhotos.forEach(photo => formData.append("photoFiles", photo));

    photos.forEach(photo => {
        if (photo instanceof File) {
            formData.append("photoOrder", `new:${newPhotos.indexOf(photo)}`);
        } else if (photo && photo.uuid) {
            formData.append("photos", photo.uuid);
            formData.append("photoOrder", photo.uuid);
        }
    });

    try {
        const response = await fetch(isEdit ? `/api/v1/offers/${technicalId}` : "/api/v1/offers", {
            method: isEdit ? "PUT" : "POST",
            body: formData
        });

        if (!response.ok) {
            throw new Error(await getResponseErrorMessage(response));
        }

        M.toast({ html: isEdit ? "Oferta zaktualizowana!" : "Oferta utworzona!", classes: "green" });
        setTimeout(() => location.reload(), 800);
    } catch (err) {
        M.toast({ html: `Błąd: ${err.message}`, classes: "red" });
        if (saveBtn) saveBtn.disabled = false;
        if (formLoader) formLoader.classList.add("hide");
    }
}

function resetForm() {
    document.getElementById("offerForm").reset();
    document.getElementById("editTechnicalId").value = "";
    photos.forEach(revokePreviewUrl);
    photos = [];
    renderPhotosPreview();
    clearSelectedPhone();
    document.getElementById("formTitle").textContent = "Stwórz nową ofertę";
    document.getElementById("cancelBtn").classList.add("hide");
    document.getElementById("phoneSelectWrapper").classList.remove("hide");
    toggleOfferForm(false);
}

async function loadOffers(page = 1) {
    page = normalizeOfferPage(page);
    currentPage = page;
    syncOfferListStateToUrl(page);
    const loader = document.getElementById("offersLoader");
    if (loader) loader.classList.remove("hide");

    try {
        const params = new URLSearchParams({
            page,
            size: 12
        });
        const imei = document.getElementById("offerImeiFilter")?.value.trim();
        const name = document.getElementById("offerNameFilter")?.value.trim();
        const location = offerLocationSelect?.getValue();

        if (imei) params.append("imei", imei);
        if (name) params.append("name", name);
        if (location) params.append("location", location);

        const response = await fetch(`/api/v1/offers?${params.toString()}`);
        if (!response.ok) {
            throw new Error(await getResponseErrorMessage(response));
        }
        const data = await response.json();

        if (data.content.length === 0 && page > 1) {
            loadOffers(page - 1);
            return;
        }

        renderOffers(data.content);
        renderPagination(data);
        renderOfferResultsMeta(data);
        setTimeout(lazyLoadImages, 100);
    } catch (err) {
        M.toast({ html: `Błąd ładowania ofert: ${err.message}`, classes: "red" });
    } finally {
        if (loader) loader.classList.add("hide");
    }
}

function lazyLoadImages() {
    document.querySelectorAll("img[data-src]").forEach(img => {
        img.src = img.dataset.src;
        img.removeAttribute("data-src");
    });
}

function renderOffers(offers) {
    const container = document.getElementById("offers-list");
    container.innerHTML = "";

    if (offers.length === 0) {
        container.innerHTML = '<div class="center-align grey-text" style="padding: 28px;">Brak aktywnych ofert dla wybranych filtrów.</div>';
        return;
    }

    offers.forEach(offer => {
        const mainPhoto = offer.photos && offer.photos.length > 0
            ? offer.photos[0].thumbnailUrl
            : "https://via.placeholder.com/300x220?text=Brak+zdjecia";
        const offerTitle = offer.phoneModelName || offer.model || offer.brand || "Oferta";

        const item = document.createElement("div");
        item.className = "offer-list-item";
        item.dataset.offerId = offer.technicalId;
        item.innerHTML = `
            <div class="offer-summary" role="button" tabindex="0" aria-expanded="false">
                <img class="offer-thumbnail" data-src="${escapeHtml(mainPhoto)}" src="https://via.placeholder.com/96x72?text=..." alt="${escapeHtml(offerTitle)}">
                <div class="offer-main">
                    <h6>${escapeHtml(offerTitle)}</h6>
                    <div class="offer-meta">
                        <span><i class="material-icons tiny">fingerprint</i> IMEI: ${escapeHtml(offer.imei || "brak")}</span>
                        <span><i class="material-icons tiny">place</i> ${escapeHtml(offer.location || "Dostępny online")}</span>
                        <span><i class="material-icons tiny">palette</i> ${escapeHtml(offer.color || "kolor nieustawiony")}</span>
                        ${offer.isReserved ? '<span class="red-text text-darken-1"><i class="material-icons tiny">event_busy</i> Zarezerwowana</span>' : ""}
                    </div>
                </div>
                <div class="offer-side">
                    <div class="offer-price">${escapeHtml(offer.price || "")} PLN</div>
                    <div class="offer-actions">
                        <button type="button" class="btn-flat waves-effect blue-text" title="Edytuj zdjęcia" onclick="editOffer('${offer.technicalId}', event)">
                            <i class="material-icons">edit</i>
                        </button>
                        <button type="button" class="btn-flat waves-effect red-text" title="Usuń ofertę" onclick="deleteOffer('${offer.technicalId}', event)">
                            <i class="material-icons">delete</i>
                        </button>
                        <i class="material-icons grey-text">expand_more</i>
                    </div>
                </div>
            </div>
            <div class="offer-details">
                <div class="offer-details-grid">
                    ${detailTile("Status", offer.status)}
                    ${detailTile("Ekran", offer.screen ? [offer.screen.size, offer.screen.resolution, offer.screen.type].filter(Boolean).join(" / ") : "")}
                    ${detailTile("Pamięć", [offer.memory, offer.ram].filter(Boolean).join(" / "))}
                    ${detailTile("SIM", offer.simCardType)}
                    ${detailTile("Bateria", [offer.batteryCapacity, offer.batteryCondition].filter(Boolean).join(" / "))}
                    ${detailTile("Aparat przedni", formatList(offer.frontCamerasMpx, "Mpx"))}
                    ${detailTile("Aparat tylny", formatList(offer.backCamerasMpx, "Mpx"))}
                    ${detailTile("Złącze", offer.communication?.portType)}
                    ${detailTile("System", offer.operatingSystem)}
                    ${detailTile("Kolor", offer.color)}
                    ${detailTile("Lokalizacja", offer.location)}
                    ${detailTile("Cena", `${offer.price || ""} PLN`)}
                </div>
                <div class="offer-photos-mini">
                    ${(offer.photos || []).map((photo, index) => `
                        <img data-src="${escapeHtml(photo.thumbnailUrl)}"
                             src="https://via.placeholder.com/72x72?text=${index + 1}"
                             onclick="openPhoto('${escapeInlineJsAttr(photo.publicUrl)}', event)"
                             alt="Zdjęcie oferty ${index + 1}">
                    `).join("")}
                </div>
            </div>
        `;

        const summary = item.querySelector(".offer-summary");
        summary.addEventListener("click", event => {
            if (event.target.closest("button")) return;
            toggleOfferDetails(item);
        });
        summary.addEventListener("keydown", event => {
            if (event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                toggleOfferDetails(item);
            }
        });

        container.appendChild(item);
    });
}

function detailTile(label, value) {
    return `
        <div class="offer-detail">
            <span>${escapeHtml(label)}</span>
            <strong>${escapeHtml(value || "-")}</strong>
        </div>
    `;
}

function toggleOfferDetails(item) {
    const expanded = item.classList.toggle("expanded");
    const summary = item.querySelector(".offer-summary");
    const icon = item.querySelector(".offer-actions > i");
    if (summary) summary.setAttribute("aria-expanded", String(expanded));
    if (icon) icon.textContent = expanded ? "expand_less" : "expand_more";
}

function openPhoto(url, event) {
    event?.stopPropagation();
    window.open(url, "_blank");
}

async function editOffer(technicalId, event) {
    event?.preventDefault();
    event?.stopPropagation();

    try {
        const response = await fetch(`/api/v1/offers/${technicalId}`);
        if (!response.ok) {
            throw new Error(await getResponseErrorMessage(response));
        }
        const offer = await response.json();

        document.getElementById("formTitle").textContent = "Edytuj zdjęcia oferty";
        document.getElementById("cancelBtn").classList.remove("hide");
        document.getElementById("editTechnicalId").value = offer.technicalId;
        photos.forEach(revokePreviewUrl);
        photos = offer.photos || [];
        renderPhotosPreview();

        document.getElementById("phoneSelectWrapper").classList.add("hide");
        document.getElementById("selectedPhoneInfo").classList.remove("hide");
        const offerTitle = offer.phoneModelName || offer.model || offer.brand || "";
        document.getElementById("phoneDetails").textContent = offerTitle;
        document.getElementById("modelSpecsPreview").classList.remove("hide");
        document.getElementById("modelSpecsPreview").innerHTML = `
            <div class="card-panel grey lighten-5 model-specs-panel">
                <div><b>Oferta:</b> ${escapeHtml(offerTitle)}</div>
                <div><b>IMEI:</b> ${escapeHtml(offer.imei || "brak")}</div>
                <div><b>Pamięć:</b> ${escapeHtml(offer.memory || "-")} / ${escapeHtml(offer.ram || "-")}</div>
                <div><b>SIM:</b> ${escapeHtml(offer.simCardType || "-")}</div>
            </div>
        `;

        toggleOfferForm(true);
        window.scrollTo({ top: 0, behavior: "smooth" });
    } catch (err) {
        M.toast({ html: `Błąd pobierania danych oferty: ${err.message}`, classes: "red" });
    }
}

async function deleteOffer(technicalId, event) {
    event?.preventDefault();
    event?.stopPropagation();
    if (!confirm("Czy na pewno chcesz usunąć tę ofertę? Zdjęcia zostaną również usunięte.")) return;

    try {
        const response = await fetch(`/api/v1/offers/${technicalId}`, { method: "DELETE" });
        if (!response.ok) throw new Error(await getResponseErrorMessage(response));

        M.toast({ html: "Oferta została usunięta", classes: "green" });
        setTimeout(() => location.reload(), 800);
    } catch (err) {
        M.toast({ html: `Błąd podczas usuwania: ${err.message}`, classes: "red" });
    }
}

function renderPagination(data) {
    const pagination = document.getElementById("offersPagination");
    if (!pagination) return;
    pagination.innerHTML = "";

    const totalPages = data.totalPages;
    const activePage = data.number;
    if (totalPages <= 1) return;

    for (let i = 0; i < totalPages; i++) {
        const li = document.createElement("li");
        li.className = i === activePage ? "active blue" : "waves-effect";
        li.innerHTML = `<a href="#!">${i + 1}</a>`;
        li.addEventListener("click", () => loadOffers(i + 1));
        pagination.appendChild(li);
    }
}

function renderOfferResultsMeta(data) {
    const meta = document.getElementById("offerResultsMeta");
    if (!meta) return;

    const total = data.totalElements ?? 0;
    if (total === 0) {
        meta.textContent = "0 ofert";
        return;
    }

    meta.textContent = `${total} ofert`;
}

function revokePreviewUrl(photo) {
    if (photo instanceof File && photo.previewUrl) {
        URL.revokeObjectURL(photo.previewUrl);
        delete photo.previewUrl;
    }
}

function formatList(values, suffix) {
    if (!values || values.length === 0) return "";
    return values.map(value => suffix ? `${value} ${suffix}` : value).join(", ");
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

async function getResponseErrorMessage(response) {
    const text = await response.text().catch(() => "");
    if (!text) {
        return response.statusText || "Nieznany błąd";
    }

    try {
        const json = JSON.parse(text);
        return json.message || json.error || text;
    } catch {
        return text;
    }
}

function escapeJs(value) {
    return String(value || "")
        .replaceAll("\\", "\\\\")
        .replaceAll("'", "\\'");
}

function escapeInlineJsAttr(value) {
    return escapeHtml(escapeJs(value));
}
