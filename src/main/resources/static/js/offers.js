let photos = [];
let phoneSelect;
let currentPage = 1;

document.addEventListener("DOMContentLoaded", function() {
    initPhoneSelect();
    loadOffers();

    document.getElementById("photoFiles").addEventListener("change", handleFileSelect);
    document.getElementById("offerForm").addEventListener("submit", saveOffer);
    document.getElementById("clearPhone").addEventListener("click", clearSelectedPhone);
    document.getElementById("cancelBtn").addEventListener("click", resetForm);
});

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
    if (photos.filter(p => p instanceof File).length + files.length > 5) {
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

        const src = photo instanceof File ? URL.createObjectURL(photo) : "https://via.placeholder.com/100x100?text=...";
        const dataSrc = photo instanceof File ? "" : photo.thumbnailUrl;

        div.innerHTML = `
            <img src="${src}" ${dataSrc ? `data-src="${dataSrc}"` : ""} alt="Foto ${index + 1}">
            <i class="material-icons remove-photo" onclick="removePhoto(${index})">close</i>
        `;
        container.appendChild(div);
    });

    if (photos.some(p => !(p instanceof File))) {
        setTimeout(lazyLoadImages, 50);
    }
}

function removePhoto(index) {
    const photo = photos[index];
    if (photo instanceof File) {
        URL.revokeObjectURL(photo);
    }
    photos.splice(index, 1);
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

    photos.forEach(photo => {
        if (photo instanceof File) {
            formData.append("photoFiles", photo);
        } else if (photo && photo.uuid) {
            formData.append("photos", photo.uuid);
        }
    });

    try {
        const response = await fetch(isEdit ? `/api/v1/offers/${technicalId}` : "/api/v1/offers", {
            method: isEdit ? "PUT" : "POST",
            body: formData
        });

        if (!response.ok) {
            const err = await response.json().catch(() => null);
            throw new Error(err?.message || "Nieznany błąd");
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
    photos = [];
    renderPhotosPreview();
    clearSelectedPhone();
    document.getElementById("formTitle").textContent = "Stwórz nową ofertę";
    document.getElementById("cancelBtn").classList.add("hide");
    document.getElementById("phoneSelectWrapper").classList.remove("hide");
}

async function loadOffers(page = 1) {
    currentPage = page;
    const loader = document.getElementById("offersLoader");
    if (loader) loader.classList.remove("hide");

    try {
        const response = await fetch(`/api/v1/external/offers?page=${page}&size=12`);
        const data = await response.json();

        if (data.content.length === 0 && page > 1) {
            loadOffers(page - 1);
            return;
        }

        renderOffers(data.content);
        renderPagination(data);
        setTimeout(lazyLoadImages, 100);
    } catch (err) {
        M.toast({ html: "Błąd ładowania ofert", classes: "red" });
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
        container.innerHTML = '<div class="col s12 center-align grey-text">Brak aktywnych ofert.</div>';
        return;
    }

    offers.forEach(offer => {
        const mainPhoto = offer.photos && offer.photos.length > 0
            ? offer.photos[0].thumbnailUrl
            : "https://via.placeholder.com/300x200?text=Brak+zdjęcia";

        const col = document.createElement("div");
        col.className = "col s12 m6 l4";
        col.innerHTML = `
            <div class="card sticky-action">
                <div class="card-image waves-effect waves-block waves-light">
                    <img class="activator" data-src="${mainPhoto}" src="https://via.placeholder.com/300x200?text=Wczytywanie..." style="height: 200px; object-fit: cover;">
                </div>
                <div class="card-content">
                    <span class="card-title activator grey-text text-darken-4">
                        ${escapeHtml(offer.brand || "")} ${escapeHtml(offer.model || "")}
                        <i class="material-icons right">more_vert</i>
                    </span>
                    <p class="blue-text text-darken-2" style="font-weight: bold; font-size: 1.2rem;">
                        ${escapeHtml(offer.price || "")} PLN
                    </p>
                </div>
                <div class="card-action">
                    <a href="#" onclick="editOffer('${offer.technicalId}')" class="blue-text">Edytuj zdjęcia</a>
                    <a href="#" onclick="deleteOffer('${offer.technicalId}')" class="red-text right">Usuń</a>
                </div>
                <div class="card-reveal">
                    <span class="card-title grey-text text-darken-4">${escapeHtml(offer.brand || "")} ${escapeHtml(offer.model || "")}<i class="material-icons right">close</i></span>
                    <p>
                        <b>Ekran:</b> ${(offer.screen && escapeHtml(offer.screen.size)) || "—"}<br>
                        <b>Bateria:</b> ${escapeHtml(offer.batteryCapacity || "—")}<br>
                        <b>Pamięć:</b> ${escapeHtml(offer.memory || "—")} / ${escapeHtml(offer.ram || "—")}<br>
                        <b>SIM:</b> ${escapeHtml(offer.simCardType || "—")}<br>
                        <b>Aparaty:</b> przód ${(offer.frontCamerasMpx || []).join(", ") || "—"}, tył ${(offer.backCamerasMpx || []).join(", ") || "—"}
                    </p>
                    <div class="offer-photos-mini">
                        ${(offer.photos || []).map(p => `<img data-src="${p.thumbnailUrl}" src="https://via.placeholder.com/50x50?text=..." onclick="window.open('${p.publicUrl}', '_blank')" style="width: 50px; height: 50px; cursor: pointer; object-fit: cover; margin-right: 5px; border-radius: 2px;">`).join("")}
                    </div>
                </div>
            </div>
        `;
        container.appendChild(col);
    });
}

async function editOffer(technicalId) {
    try {
        const response = await fetch(`/api/v1/external/offers/${technicalId}`);
        const offer = await response.json();

        document.getElementById("formTitle").textContent = "Edytuj zdjęcia oferty";
        document.getElementById("cancelBtn").classList.remove("hide");
        document.getElementById("editTechnicalId").value = offer.technicalId;
        photos = offer.photos || [];
        renderPhotosPreview();

        document.getElementById("phoneSelectWrapper").classList.add("hide");
        document.getElementById("selectedPhoneInfo").classList.remove("hide");
        document.getElementById("phoneDetails").textContent = `${offer.brand || ""} ${offer.model || ""}`;
        document.getElementById("modelSpecsPreview").classList.remove("hide");
        document.getElementById("modelSpecsPreview").innerHTML = `
            <div class="card-panel grey lighten-5 model-specs-panel">
                <div><b>Oferta:</b> ${escapeHtml(offer.brand || "")} ${escapeHtml(offer.model || "")}</div>
                <div><b>Pamięć:</b> ${escapeHtml(offer.memory || "—")} / ${escapeHtml(offer.ram || "—")}</div>
                <div><b>SIM:</b> ${escapeHtml(offer.simCardType || "—")}</div>
            </div>
        `;

        window.scrollTo({ top: 0, behavior: "smooth" });
    } catch (err) {
        M.toast({ html: "Błąd pobierania danych oferty", classes: "red" });
    }
}

async function deleteOffer(technicalId) {
    if (!confirm("Czy na pewno chcesz usunąć tę ofertę? Zdjęcia zostaną również usunięte.")) return;

    try {
        const response = await fetch(`/api/v1/offers/${technicalId}`, { method: "DELETE" });
        if (!response.ok) throw new Error("Błąd usuwania");

        M.toast({ html: "Oferta została usunięta", classes: "green" });
        setTimeout(() => location.reload(), 800);
    } catch (err) {
        M.toast({ html: "Błąd podczas usuwania", classes: "red" });
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

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}
