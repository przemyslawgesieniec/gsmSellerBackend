const modelsPageSize = 20;
let modelsCurrentPage = 0;
let modelToDelete = null;

document.addEventListener("DOMContentLoaded", () => {
    M.Modal.init(document.querySelectorAll(".modal"));
    M.FormSelect.init(document.querySelectorAll("select"));

    document.getElementById("openCreateModelBtn").addEventListener("click", openCreateModelModal);
    document.getElementById("saveModelBtn").addEventListener("click", saveModel);
    document.getElementById("confirmDeleteModelBtn").addEventListener("click", deleteModel);
    document.getElementById("modelSearch").addEventListener("input", debounce(() => loadModels(0), 300));
    document.getElementById("addMemoryBtn").addEventListener("click", () => addNumericField("memoryFields", "Pamięć"));
    document.getElementById("addRamBtn").addEventListener("click", () => addNumericField("ramFields", "RAM"));
    document.getElementById("addColorBtn").addEventListener("click", () => addDynamicField("colorsFields", "Kolor"));
    document.getElementById("addFrontCameraBtn").addEventListener("click", () => addNumericField("frontCameraFields", "Aparat przód (Mpx)"));
    document.getElementById("addBackCameraBtn").addEventListener("click", () => addNumericField("backCameraFields", "Aparat tył (Mpx)"));

    loadModels(0);
});

async function loadModels(page = 0) {
    modelsCurrentPage = page;
    const search = document.getElementById("modelSearch").value.trim();
    const params = new URLSearchParams({
        page,
        size: modelsPageSize,
        search
    });

    showModelsLoader(true);

    try {
        const response = await fetch(`/api/v1/phone-models?${params.toString()}`);
        if (!response.ok) throw new Error("Błąd pobierania modeli");

        const data = await response.json();
        renderModels(data.content || []);
        renderModelsPagination(data);
    } catch (e) {
        M.toast({ html: "Nie udało się pobrać modeli", classes: "red" });
    } finally {
        showModelsLoader(false);
    }
}

function renderModels(models) {
    const tbody = document.getElementById("modelsTableBody");
    const empty = document.getElementById("modelsEmptyState");
    tbody.innerHTML = "";

    empty.classList.toggle("hide", models.length > 0);

    models.forEach(model => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td>
                <strong>${escapeHtml(model.brand || "")} ${escapeHtml(model.model || "")}</strong>
                <br>
                <span class="grey-text">${escapeHtml(model.displayName || "")}</span>
            </td>
            <td>
                ${escapeHtml(model.screen || "—")}
                ${model.screenResolution ? `<br><span class="grey-text">${escapeHtml(model.screenResolution)}</span>` : ""}
                ${model.displayType ? `<br><span class="grey-text">${escapeHtml(model.displayType)}</span>` : ""}
            </td>
            <td>${escapeHtml([model.memory, model.ram].filter(Boolean).join(" / ") || "—")}</td>
            <td>
                ${escapeHtml(model.simCardType || "—")}
                ${model.portType ? `<br><span class="grey-text">Port: ${escapeHtml(model.portType)}</span>` : ""}
                ${model.dualSim ? '<br><span class="badge blue white-text">Dual SIM</span>' : ""}
            </td>
            <td>${escapeHtml(model.colors || "—")}</td>
            <td>
                <span class="grey-text">Przód:</span> ${escapeHtml(model.frontCameras || "—")}
                <br>
                <span class="grey-text">Tył:</span> ${escapeHtml(model.backCameras || "—")}
            </td>
            <td>${escapeHtml(model.batteryCapacity || "—")}</td>
            <td class="right-align">
                <button class="btn-small blue darken-2" data-action="edit" data-id="${model.technicalId}">
                    <i class="material-icons">edit</i>
                </button>
                <button class="btn-small red darken-2" data-action="delete" data-id="${model.technicalId}">
                    <i class="material-icons">delete</i>
                </button>
            </td>
        `;

        tr.querySelector('[data-action="edit"]').addEventListener("click", () => openEditModelModal(model));
        tr.querySelector('[data-action="delete"]').addEventListener("click", () => openDeleteModelModal(model.technicalId));
        tbody.appendChild(tr);
    });
}

function openCreateModelModal() {
    resetModelForm();
    document.getElementById("modelModalTitle").textContent = "Dodaj model";
    M.Modal.getInstance(document.getElementById("modelModal")).open();
}

function openEditModelModal(model) {
    document.getElementById("modelModalTitle").textContent = "Edytuj model";
    document.getElementById("modelTechnicalId").value = model.technicalId || "";
    document.getElementById("brand").value = model.brand || "";
    document.getElementById("model").value = model.model || "";
    document.getElementById("screen").value = model.screen || "";
    document.getElementById("screenResolution").value = model.screenResolution || "";
    document.getElementById("displayType").value = model.displayType || "";
    document.getElementById("simCardType").value = model.simCardType || "";
    document.getElementById("portType").value = model.portType || "";
    document.getElementById("dualSim").checked = model.dualSim === true;
    document.getElementById("batteryCapacity").value = model.batteryCapacity || "";
    setNumericFields("memoryFields", "Pamięć", splitCommaList(model.memory));
    setNumericFields("ramFields", "RAM", splitCommaList(model.ram));
    setDynamicFields("colorsFields", "Kolor", splitCommaList(model.colors));
    setNumericFields("frontCameraFields", "Aparat przód (Mpx)", splitCommaList(model.frontCameras));
    setNumericFields("backCameraFields", "Aparat tył (Mpx)", splitCommaList(model.backCameras));

    M.FormSelect.init(document.querySelectorAll("select"));
    M.updateTextFields();
    M.Modal.getInstance(document.getElementById("modelModal")).open();
}

async function saveModel() {
    const form = document.getElementById("modelForm");
    if (!form.reportValidity()) return;
    if (!validateNumericModelFields()) return;

    const technicalId = document.getElementById("modelTechnicalId").value;
    const payload = {
        brand: document.getElementById("brand").value.trim(),
        model: document.getElementById("model").value.trim(),
        screen: document.getElementById("screen").value.trim(),
        screenResolution: document.getElementById("screenResolution").value.trim(),
        displayType: document.getElementById("displayType").value.trim(),
        memory: collectDynamicFields("memoryFields"),
        ram: collectDynamicFields("ramFields"),
        simCardType: document.getElementById("simCardType").value.trim(),
        portType: document.getElementById("portType").value,
        dualSim: document.getElementById("dualSim").checked,
        colors: collectDynamicFields("colorsFields"),
        frontCameras: collectDynamicFields("frontCameraFields"),
        backCameras: collectDynamicFields("backCameraFields"),
        batteryCapacity: document.getElementById("batteryCapacity").value.trim()
    };

    try {
        const response = await fetch(technicalId ? `/api/v1/phone-models/${technicalId}` : "/api/v1/phone-models", {
            method: technicalId ? "PUT" : "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });

        if (!response.ok) throw new Error("Błąd zapisu modelu");

        M.toast({ html: technicalId ? "Model zaktualizowany" : "Model dodany", classes: "green" });
        M.Modal.getInstance(document.getElementById("modelModal")).close();
        loadModels(modelsCurrentPage);
    } catch (e) {
        M.toast({ html: "Nie udało się zapisać modelu", classes: "red" });
    }
}

function openDeleteModelModal(technicalId) {
    modelToDelete = technicalId;
    M.Modal.getInstance(document.getElementById("deleteModelModal")).open();
}

async function deleteModel() {
    if (!modelToDelete) return;

    try {
        const response = await fetch(`/api/v1/phone-models/${modelToDelete}`, { method: "DELETE" });
        if (!response.ok) {
            const error = await response.json().catch(() => null);
            throw new Error(error?.message || "Nie można usunąć modelu");
        }

        M.toast({ html: "Model usunięty", classes: "green" });
        M.Modal.getInstance(document.getElementById("deleteModelModal")).close();
        loadModels(modelsCurrentPage);
    } catch (e) {
        M.toast({ html: e.message || "Nie udało się usunąć modelu", classes: "red" });
    }
}

function resetModelForm() {
    document.getElementById("modelForm").reset();
    document.getElementById("modelTechnicalId").value = "";
    document.getElementById("dualSim").checked = false;
    document.getElementById("portType").value = "";
    setNumericFields("memoryFields", "Pamięć", [""]);
    setNumericFields("ramFields", "RAM", [""]);
    setDynamicFields("colorsFields", "Kolor", [""]);
    setNumericFields("frontCameraFields", "Aparat przód (Mpx)", [""]);
    setNumericFields("backCameraFields", "Aparat tył (Mpx)", [""]);
    M.FormSelect.init(document.querySelectorAll("select"));
    M.updateTextFields();
}

function addNumericField(containerId, label, value = "") {
    addDynamicField(containerId, label, value, {
        type: "number",
        min: "0",
        step: "1",
        numeric: true
    });
}

function addDynamicField(containerId, label, value = "", options = {}) {
    const container = document.getElementById(containerId);
    const row = document.createElement("div");
    row.className = "dynamic-field-row";
    row.innerHTML = `
        <div class="input-field dynamic-field-input">
            <input type="${options.type || "text"}"
                   value="${escapeHtml(value)}"
                   ${options.min != null ? `min="${options.min}"` : ""}
                   ${options.step != null ? `step="${options.step}"` : ""}
                   ${options.numeric ? 'inputmode="numeric" pattern="[0-9]*"' : ""}>
            <label class="${value ? "active" : ""}">${escapeHtml(label)}</label>
        </div>
        <button type="button" class="btn-flat red-text dynamic-remove-btn" title="Usuń">
            <i class="material-icons">close</i>
        </button>
    `;

    row.querySelector(".dynamic-remove-btn").addEventListener("click", () => {
        row.remove();
        if (container.children.length === 0) {
            addDynamicField(containerId, label);
        }
    });

    container.appendChild(row);
    M.updateTextFields();
}

function setDynamicFields(containerId, label, values) {
    const container = document.getElementById(containerId);
    container.innerHTML = "";
    const normalized = values.length > 0 ? values : [""];
    normalized.forEach(value => addDynamicField(containerId, label, value));
}

function setNumericFields(containerId, label, values) {
    const container = document.getElementById(containerId);
    container.innerHTML = "";
    const normalized = values.length > 0 ? values : [""];
    normalized.forEach(value => addNumericField(containerId, label, value));
}

function collectDynamicFields(containerId) {
    return Array.from(document.querySelectorAll(`#${containerId} input`))
        .map(input => input.value.trim())
        .filter(Boolean)
        .join(",");
}

function splitCommaList(value) {
    if (!value) return [];
    return value.split(",")
        .map(item => item.trim())
        .filter(Boolean);
}

function validateNumericModelFields() {
    const battery = document.getElementById("batteryCapacity");
    const numericInputs = [
        battery,
        ...document.querySelectorAll("#memoryFields input, #ramFields input, #frontCameraFields input, #backCameraFields input")
    ];

    const invalid = numericInputs.find(input => {
        const value = input.value.trim();
        return value !== "" && !/^\d+$/.test(value);
    });

    if (invalid) {
        invalid.focus();
        M.toast({ html: "Pamięć, RAM, aparaty i bateria mogą zawierać tylko liczby", classes: "red" });
        return false;
    }

    return true;
}

function showModelsLoader(show) {
    document.getElementById("modelsLoader").classList.toggle("hide", !show);
}

function renderModelsPagination(data) {
    const pagination = document.getElementById("modelsPagination");
    pagination.innerHTML = "";

    if (!data || data.totalPages <= 1) return;

    for (let page = 0; page < data.totalPages; page++) {
        const li = document.createElement("li");
        li.className = page === data.number ? "active blue" : "waves-effect";
        li.innerHTML = `<a href="#!">${page + 1}</a>`;
        li.addEventListener("click", () => loadModels(page));
        pagination.appendChild(li);
    }
}

function debounce(fn, delay = 300) {
    let timeout;
    return (...args) => {
        clearTimeout(timeout);
        timeout = setTimeout(() => fn(...args), delay);
    };
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}
