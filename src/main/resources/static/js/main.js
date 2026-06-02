const pageSize = 20;
let currentPage = 0;
let phoneToHandover = null;
let phoneToReserve = null;
let phoneToAssignModel = null;
let assignPhoneModelSelect = null;
let assignRamSelect = null;
let assignMemorySelect = null;
let assignColorSelect = null;
let assignSimCardTypeSelect = null;
let assignVariantRequestId = 0;
let stockStatusSelect = null;
let stockLocationSelect = null;
let stockBrandSelect = null;
let stockModelSelect = null;
let stockMemorySelect = null;
let isRestoringStockListState = false;
let stockBrandOptions = [];
let stockModelOptionsByBrand = {};
let stockMemoryOptions = [];
let stockLoadRequestId = 0;
let stockLoadAbortController = null;
const stockListStateStorageKey = "gsm-seller.stock-list-state.v1";

const listContainer = document.getElementById("phone-list");
const stockFilterFieldIds = [
    "filterName",
    "filterColor",
    "filterImei",
    "filterPriceMin",
    "filterPriceMax"
];
const stockBooleanFilterIds = [
    "filterHasOffer",
    "filterAfterService"
];

function formatPhoneModelOption(item) {
    const spec = [item.memory, item.ram, item.colors, item.simCardType].filter(Boolean).join(" / ");
    return `${item.displayName || `${item.brand || ""} ${item.model || ""}`.trim()}${spec ? ` (${spec})` : ""}`;
}

function formatStockModelFilterOption(item) {
    return formatPhoneModelOption(item);
}

function formatStockModelFilterControlValue(item) {
    return item.displayName || item.model || "";
}

function splitModelOptions(value) {
    if (!value) return [];
    return String(value)
        .split(",")
        .map(item => item.trim())
        .filter(Boolean);
}

function findMatchingOption(options, currentValue) {
    if (!currentValue) return "";
    const normalizedCurrent = String(currentValue).trim().toLowerCase();
    const currentDigits = String(currentValue).replace(/\D/g, "");

    return options.find(option => option.toLowerCase() === normalizedCurrent)
        || options.find(option => currentDigits && option.replace(/\D/g, "") === currentDigits)
        || "";
}

function initAssignVariantSelect(selector) {
    const el = document.querySelector(selector);
    if (!el) return null;

    return new TomSelect(el, {
        controlInput: null,
        create: false,
        searchField: [],
        shouldSort: false,
        hideSelected: true,
        closeAfterSelect: true,
        allowEmptyOption: true
    });
}

function setAssignVariantOptions(ts, options, currentValue, emptyText) {
    if (!ts) return;

    const uniqueOptions = [...new Set(options)];
    const matchedValue = findMatchingOption(uniqueOptions, currentValue);

    ts.clear(true);
    ts.clearOptions();
    ts.addOption({ value: "", text: emptyText });
    uniqueOptions.forEach(option => ts.addOption({ value: option, text: option }));
    ts.refreshOptions(false);

    if (matchedValue) {
        ts.setValue(matchedValue, true);
    }
}

function updateAssignVariantOptions(model, card) {
    setAssignVariantOptions(
        assignRamSelect,
        splitModelOptions(model?.ram),
        card?.dataset.ram || "",
        "Wybierz RAM"
    );
    setAssignVariantOptions(
        assignMemorySelect,
        splitModelOptions(model?.memory),
        card?.dataset.memory || "",
        "Wybierz pamięć"
    );
    setAssignVariantOptions(
        assignColorSelect,
        splitModelOptions(model?.colors),
        card?.dataset.color || "",
        "Wybierz kolor"
    );
    setAssignVariantOptions(
        assignSimCardTypeSelect,
        splitModelOptions(model?.simCardType),
        card?.dataset.simCardType || "",
        "Wybierz SIM"
    );
}

function getAssignModelVariantContainer() {
    return document.getElementById("assignModelVariantFields");
}

function destroyAssignVariantSelects() {
    [assignRamSelect, assignMemorySelect, assignColorSelect, assignSimCardTypeSelect].forEach(select => {
        if (select) {
            select.destroy();
        }
    });
    assignRamSelect = null;
    assignMemorySelect = null;
    assignColorSelect = null;
    assignSimCardTypeSelect = null;
}

function clearAssignVariantFields(message = "") {
    destroyAssignVariantSelects();
    const container = getAssignModelVariantContainer();
    if (!container) return;

    container.innerHTML = message
        ? `<div class="assign-model-variants-message grey-text">${message}</div>`
        : "";
}

function renderAssignVariantFields(model, card) {
    const container = getAssignModelVariantContainer();
    if (!container) return;

    destroyAssignVariantSelects();
    container.innerHTML = `
        <div class="row" style="margin-bottom: 0;">
            <div class="input-field col s12 m3">
                <select id="assignRamSelect"></select>
                <label class="active">RAM</label>
            </div>
            <div class="input-field col s12 m3">
                <select id="assignMemorySelect"></select>
                <label class="active">Pamięć</label>
            </div>
            <div class="input-field col s12 m3">
                <select id="assignColorSelect"></select>
                <label class="active">Kolor</label>
            </div>
            <div class="input-field col s12 m3">
                <select id="assignSimCardTypeSelect"></select>
                <label class="active">SIM</label>
            </div>
        </div>
    `;

    assignRamSelect = initAssignVariantSelect('#assignRamSelect');
    assignMemorySelect = initAssignVariantSelect('#assignMemorySelect');
    assignColorSelect = initAssignVariantSelect('#assignColorSelect');
    assignSimCardTypeSelect = initAssignVariantSelect('#assignSimCardTypeSelect');
    updateAssignVariantOptions(model, card);
}

function currentAssignModelCard() {
    return phoneToAssignModel
        ? document.querySelector(`.card[data-technical-id="${phoneToAssignModel}"]`)
        : null;
}

async function loadAssignModelVariants(phoneModelTechnicalId, card = currentAssignModelCard()) {
    const requestId = ++assignVariantRequestId;
    clearAssignVariantFields(phoneModelTechnicalId ? "Ładowanie opcji modelu..." : "");

    if (!phoneModelTechnicalId) return;

    try {
        const response = await fetch(`/api/v1/phone-models/${encodeURIComponent(phoneModelTechnicalId)}`);
        if (!response.ok) throw new Error("Nie udało się pobrać modelu");

        const model = await response.json();
        if (requestId !== assignVariantRequestId) return;

        renderAssignVariantFields(model, card);
    } catch (e) {
        if (requestId !== assignVariantRequestId) return;

        clearAssignVariantFields();
        M.toast({ html: "Nie udało się pobrać opcji RAM, pamięci, koloru i SIM", classes: "red" });
    }
}

function initPhoneModelSelect(selector) {
    const el = document.querySelector(selector);
    if (!el) return null;

    return new TomSelect(el, {
        valueField: "technicalId",
        labelField: "displayName",
        searchField: ["displayName", "brand", "model", "memory", "ram", "colors", "simCardType"],
        create: false,
        shouldSort: false,
        hideSelected: true,
        closeAfterSelect: true,
        allowEmptyOption: true,
        render: {
            option: function(item, escape) {
                return `<div>
                    <span><b>${escape(item.brand || "")}</b> ${escape(item.model || "")}</span><br>
                    <small class="grey-text">${escape([item.memory, item.ram, item.colors, item.simCardType].filter(Boolean).join(" / "))}</small>
                </div>`;
            },
            item: function(item, escape) {
                return `<div>${escape(item.displayName || formatPhoneModelOption(item))}</div>`;
            }
        }
    });
}

async function loadAssignPhoneModelOptions(selectedValue = "", selectedLabel = "") {
    if (!assignPhoneModelSelect) {
        assignPhoneModelSelect = initPhoneModelSelect('#assignPhoneModelSelect');
    }

    if (!assignPhoneModelSelect) return;

    assignPhoneModelSelect.clear(true);
    assignPhoneModelSelect.clearOptions();
    assignPhoneModelSelect.addOption({ technicalId: "", displayName: "Wybierz model z bazy" });

    try {
        const response = await fetch("/api/v1/phone-models?size=1000");
        if (!response.ok) throw new Error("Nie udało się pobrać modeli");

        const data = await response.json();
        (data.content || []).forEach(model => {
            assignPhoneModelSelect.addOption({
                ...model,
                displayName: formatPhoneModelOption(model)
            });
        });

        if (
            selectedValue
            && !assignPhoneModelSelect.options[selectedValue]
        ) {
            const selectedResponse = await fetch(`/api/v1/phone-models/${selectedValue}`);
            if (selectedResponse.ok) {
                const model = await selectedResponse.json();
                assignPhoneModelSelect.addOption({
                    ...model,
                    displayName: formatPhoneModelOption(model)
                });
            } else {
                assignPhoneModelSelect.addOption({
                    technicalId: selectedValue,
                    displayName: selectedLabel || selectedValue
                });
            }
        }

        assignPhoneModelSelect.refreshOptions(false);
        if (selectedValue) {
            assignPhoneModelSelect.setValue(selectedValue, true);
        }
    } catch (e) {
        M.toast({ html: "Nie udało się pobrać modeli z bazy", classes: "red" });
    }
}

function getFilters() {
    return {
        name: document.getElementById("filterName").value.trim(),
        brand: document.getElementById("filterBrand").value.trim(),
        model: document.getElementById("filterModel").value.trim(),
        memory: document.getElementById("filterMemory").value.trim(),
        color: document.getElementById("filterColor").value.trim(),
        imei: document.getElementById("filterImei").value.trim(),
        priceMin: document.getElementById("filterPriceMin").value.trim(),
        priceMax: document.getElementById("filterPriceMax").value.trim(),
        status: document.getElementById("filterStatus").value.trim(),
        locationName: document.getElementById("filterLocation").value.trim(),
        hasOffer: document.getElementById("filterHasOffer").checked,
        afterService: document.getElementById("filterAfterService").checked ? true : null
    };
}

function normalizeStockPageIndex(page) {
    const parsed = Number.parseInt(page, 10);
    return Number.isInteger(parsed) && parsed >= 0 ? parsed : 0;
}

function normalizeStockUrlPage(page) {
    const parsed = Number.parseInt(page, 10);
    return Number.isInteger(parsed) && parsed > 0 ? parsed : 1;
}

function emptyStockFilters() {
    return {
        name: "",
        brand: "",
        model: "",
        memory: "",
        color: "",
        imei: "",
        priceMin: "",
        priceMax: "",
        status: "",
        locationName: "",
        hasOffer: false,
        afterService: null
    };
}

function normalizeStockFilters(filters = {}) {
    return {
        ...emptyStockFilters(),
        ...filters,
        hasOffer: filters.hasOffer === true || filters.hasOffer === "true",
        afterService: filters.afterService === true || filters.afterService === "true" ? true : null
    };
}

function getStockListStateFromUrl() {
    const params = new URLSearchParams(window.location.search);
    const stateKeys = [
        "page",
        "name",
        "brand",
        "model",
        "memory",
        "color",
        "imei",
        "priceMin",
        "priceMax",
        "status",
        "locationName",
        "hasOffer",
        "afterService"
    ];

    if (!stateKeys.some(key => params.has(key))) {
        return null;
    }

    return {
        page: normalizeStockUrlPage(params.get("page")) - 1,
        filters: normalizeStockFilters({
            name: params.get("name") || "",
            brand: params.get("brand") || "",
            model: params.get("model") || "",
            memory: params.get("memory") || "",
            color: params.get("color") || "",
            imei: params.get("imei") || "",
            priceMin: params.get("priceMin") || "",
            priceMax: params.get("priceMax") || "",
            status: params.get("status") || "",
            locationName: params.get("locationName") || "",
            hasOffer: params.get("hasOffer") === "true",
            afterService: params.get("afterService") === "true"
        })
    };
}

function getStoredStockListState() {
    try {
        const rawState = sessionStorage.getItem(stockListStateStorageKey);
        if (!rawState) return null;

        const state = JSON.parse(rawState);
        return {
            page: normalizeStockPageIndex(state.page),
            filters: normalizeStockFilters(state.filters)
        };
    } catch (e) {
        sessionStorage.removeItem(stockListStateStorageKey);
        return null;
    }
}

function applyStockListState(state) {
    const filters = normalizeStockFilters(state?.filters);
    const fieldParamMap = {
        filterName: "name",
        filterColor: "color",
        filterImei: "imei",
        filterPriceMin: "priceMin",
        filterPriceMax: "priceMax"
    };

    isRestoringStockListState = true;
    try {
        Object.entries(fieldParamMap).forEach(([id, key]) => {
            const el = document.getElementById(id);
            if (el) el.value = filters[key] || "";
        });

        let brand = resolveStockBrandValue(filters.brand || "");
        const model = resolveStockModelValue(filters.model || "", brand);
        if (!brand && model) {
            brand = findBrandForModel(model);
        }

        setTomSelectValue("#filterBrand", brand);
        updateStockModelOptions(brand, model);
        setTomSelectValue("#filterMemory", filters.memory || "");

        const hasOffer = document.getElementById("filterHasOffer");
        const afterService = document.getElementById("filterAfterService");
        if (hasOffer) hasOffer.checked = Boolean(filters.hasOffer);
        if (afterService) afterService.checked = filters.afterService === true;

        setTomSelectValue("#filterStatus", filters.status || "");
        setTomSelectValue("#filterLocation", filters.locationName || "");

        M.updateTextFields();
    } finally {
        isRestoringStockListState = false;
    }
}

function restoreStockListState() {
    const state = getStockListStateFromUrl() || getStoredStockListState() || {
        page: 0,
        filters: emptyStockFilters()
    };

    applyStockListState(state);
    persistStockListState(state.page, state.filters);
    return normalizeStockPageIndex(state.page);
}

function setTomSelectValue(selector, value) {
    const element = typeof selector === "string"
        ? document.querySelector(selector)
        : selector;
    const select = getTomSelectInstance(selector);
    if (!select) {
        if (element) element.value = value || "";
        return;
    }

    if (value && !select.options[value]) {
        select.addOption({ value, text: value });
    }
    select.setValue(value, true);
}

function getTomSelectInstance(selectorOrElement) {
    const element = typeof selectorOrElement === "string"
        ? document.querySelector(selectorOrElement)
        : selectorOrElement;

    return element?.tomselect || null;
}

function syncStockListStateToUrl(page = currentPage, filters = getFilters()) {
    const url = new URL(window.location.href);
    filters = normalizeStockFilters(filters);

    url.searchParams.set("page", String(normalizeStockPageIndex(page) + 1));
    setOrDeleteStockSearchParam(url.searchParams, "name", filters.name);
    setOrDeleteStockSearchParam(url.searchParams, "brand", filters.brand);
    setOrDeleteStockSearchParam(url.searchParams, "model", filters.model);
    setOrDeleteStockSearchParam(url.searchParams, "memory", filters.memory);
    setOrDeleteStockSearchParam(url.searchParams, "color", filters.color);
    setOrDeleteStockSearchParam(url.searchParams, "imei", filters.imei);
    setOrDeleteStockSearchParam(url.searchParams, "priceMin", filters.priceMin);
    setOrDeleteStockSearchParam(url.searchParams, "priceMax", filters.priceMax);
    setOrDeleteStockSearchParam(url.searchParams, "status", filters.status);
    setOrDeleteStockSearchParam(url.searchParams, "locationName", filters.locationName);
    setOrDeleteStockSearchParam(url.searchParams, "hasOffer", filters.hasOffer ? "true" : "");
    setOrDeleteStockSearchParam(url.searchParams, "afterService", filters.afterService ? "true" : "");

    window.history.replaceState({}, "", url);
}

function persistStockListState(page = currentPage, filters = getFilters()) {
    const state = {
        page: normalizeStockPageIndex(page),
        filters: normalizeStockFilters(filters)
    };

    try {
        sessionStorage.setItem(stockListStateStorageKey, JSON.stringify(state));
    } catch (e) {
        // Brak dostępu do sessionStorage nie powinien blokować pracy listy.
    }

    syncStockListStateToUrl(state.page, state.filters);
    return state;
}

function refreshStockListPreservingState() {
    const state = persistStockListState(currentPage, getFilters());
    applyStockListState(state);
    return loadStock(state.page, state.filters);
}

function setOrDeleteStockSearchParam(params, key, value) {
    if (value) {
        params.set(key, value);
    } else {
        params.delete(key);
    }
}

function initStockModelFilterSelects() {
    stockBrandSelect = initSimpleSelect('#filterBrand');
    stockModelSelect = initSimpleSelect('#filterModel');
    stockMemorySelect = initSimpleSelect('#filterMemory');

    if (stockBrandSelect) {
        stockBrandSelect.on('change', brand => {
            updateStockModelOptions(brand, "");
            if (!isRestoringStockListState) liveReload();
        });
    }

    if (stockModelSelect) {
        stockModelSelect.on('change', () => {
            if (!isRestoringStockListState) liveReload();
        });
    }

    if (stockMemorySelect) {
        stockMemorySelect.on('change', () => {
            if (!isRestoringStockListState) liveReload();
        });
    }
}

async function loadStockModelFilterOptions() {
    try {
        const response = await fetch("/api/v1/phone-models/filter-options");
        if (!response.ok) throw new Error("Nie udało się pobrać modeli");

        const data = await response.json();
        stockBrandOptions = data.brands || [];
        stockModelOptionsByBrand = data.modelsByBrand || {};
        stockMemoryOptions = buildStockMemoryOptions(stockModelOptionsByBrand);
        updateStockBrandOptions();
        updateStockModelOptions(stockBrandSelect?.getValue() || "", stockModelSelect?.getValue() || "");
        updateStockMemoryOptions(stockMemorySelect?.getValue() || "");
    } catch (e) {
        console.warn("Nie udało się pobrać marek i modeli telefonów", e);
        M.toast({ html: "Nie udało się pobrać marek i modeli", classes: "red" });
    }
}

function buildStockMemoryOptions(modelsByBrand) {
    const optionsByNormalizedValue = new Map();

    Object.values(modelsByBrand)
        .flat()
        .flatMap(model => splitModelOptions(model.memory))
        .forEach(memory => {
            const normalizedMemory = normalizeStockMemoryValue(memory);
            if (normalizedMemory && !optionsByNormalizedValue.has(normalizedMemory)) {
                optionsByNormalizedValue.set(normalizedMemory, memory);
            }
        });

    return [...optionsByNormalizedValue.values()]
        .sort(compareStockMemoryOptions);
}

function normalizeStockMemoryValue(value) {
    return String(value || "").trim().toLowerCase().replace(/\s+/g, "");
}

function compareStockMemoryOptions(a, b) {
    const aNumber = Number.parseInt(String(a).replace(/\D/g, ""), 10);
    const bNumber = Number.parseInt(String(b).replace(/\D/g, ""), 10);

    if (Number.isInteger(aNumber) && Number.isInteger(bNumber) && aNumber !== bNumber) {
        return aNumber - bNumber;
    }

    return String(a).localeCompare(String(b), "pl", { sensitivity: "base", numeric: true });
}

function updateStockMemoryOptions(selectedMemory = "") {
    if (!stockMemorySelect) return;

    const resolvedMemory = resolveStockMemoryValue(selectedMemory);
    stockMemorySelect.clear(true);
    stockMemorySelect.clearOptions();
    stockMemorySelect.addOption({ value: "", text: "Dowolna" });

    stockMemoryOptions.forEach(memory => stockMemorySelect.addOption({
        value: memory,
        text: memory
    }));

    if (resolvedMemory && !stockMemorySelect.options[resolvedMemory]) {
        stockMemorySelect.addOption({ value: resolvedMemory, text: resolvedMemory });
    }

    stockMemorySelect.refreshOptions(false);
    stockMemorySelect.setValue(resolvedMemory || "", true);
}

function updateStockBrandOptions() {
    if (!stockBrandSelect) return;

    const selectedBrand = resolveStockBrandValue(stockBrandSelect.getValue());
    stockBrandSelect.clearOptions();
    stockBrandSelect.addOption({ value: "", text: "Dowolna" });

    stockBrandOptions
        .slice()
        .sort((a, b) => (a.name || "").localeCompare(b.name || "", "pl", { sensitivity: "base" }))
        .forEach(brand => stockBrandSelect.addOption({ value: brand.technicalId, text: brand.name }));

    stockBrandSelect.refreshOptions(false);
    if (selectedBrand && stockBrandSelect.options[selectedBrand]) {
        stockBrandSelect.setValue(selectedBrand, true);
    }
}

function updateStockModelOptions(brand, selectedModel = "") {
    if (!stockModelSelect) return;

    brand = resolveStockBrandValue(brand);
    selectedModel = resolveStockModelValue(selectedModel, brand);
    const models = brand ? (stockModelOptionsByBrand[brand] || []) : [];

    stockModelSelect.clear(true);
    stockModelSelect.clearOptions();

    if (!brand) {
        stockModelSelect.addOption({ value: "", text: "Wybierz markę" });
        stockModelSelect.disable();
        stockModelSelect.refreshOptions(false);
        return;
    }

    stockModelSelect.enable();
    stockModelSelect.addOption({ value: "", text: "Dowolny" });
    models
        .slice()
        .sort((a, b) => (a.displayName || a.model || "").localeCompare(b.displayName || b.model || "", "pl", { sensitivity: "base", numeric: true }))
        .forEach(model => stockModelSelect.addOption({
            value: model.technicalId,
            text: formatStockModelFilterControlValue(model)
        }));

    if (selectedModel && !stockModelSelect.options[selectedModel]) {
        stockModelSelect.addOption({ value: selectedModel, text: selectedModel });
    }

    stockModelSelect.refreshOptions(false);
    stockModelSelect.setValue(selectedModel || "", true);
}

function resolveStockBrandValue(value) {
    if (!value) return "";

    const normalizedValue = String(value).trim().toLowerCase();
    const match = stockBrandOptions.find(brand =>
        String(brand.technicalId) === String(value)
        || (brand.name || "").trim().toLowerCase() === normalizedValue
    );

    return match?.technicalId || value;
}

function resolveStockModelValue(value, brand = "") {
    if (!value) return "";

    const normalizedValue = String(value).trim().toLowerCase();
    const groups = brand
        ? [stockModelOptionsByBrand[brand] || []]
        : Object.values(stockModelOptionsByBrand);
    const match = groups
        .flat()
        .find(model =>
            String(model.technicalId) === String(value)
            || (model.model || "").trim().toLowerCase() === normalizedValue
            || (model.displayName || "").trim().toLowerCase() === normalizedValue
        );

    return match?.technicalId || value;
}

function resolveStockMemoryValue(value) {
    if (!value) return "";

    const normalizedValue = normalizeStockMemoryValue(value);
    return stockMemoryOptions.find(memory => normalizeStockMemoryValue(memory) === normalizedValue) || value;
}

function findStockBrandOption(value) {
    if (!value) return null;

    const normalizedValue = String(value).trim().toLowerCase();
    return stockBrandOptions.find(brand =>
        String(brand.technicalId) === String(value)
        || (brand.name || "").trim().toLowerCase() === normalizedValue
    ) || null;
}

function findStockModelOption(value) {
    if (!value) return null;

    const normalizedValue = String(value).trim().toLowerCase();
    return Object.values(stockModelOptionsByBrand)
        .flat()
        .find(model =>
            String(model.technicalId) === String(value)
            || (model.model || "").trim().toLowerCase() === normalizedValue
            || (model.displayName || "").trim().toLowerCase() === normalizedValue
        ) || null;
}

function findBrandForModel(model) {
    if (!model) return "";

    const normalizedModel = model.trim().toLowerCase();
    const match = Object.entries(stockModelOptionsByBrand)
        .find(([, models]) => models.some(option =>
            String(option.technicalId) === String(model)
            || (option.model || "").trim().toLowerCase() === normalizedModel
            || (option.displayName || "").trim().toLowerCase() === normalizedModel
        ));

    return match ? match[0] : "";
}

function getStockFilterDisplayValue(key, value) {
    if (key === "brand") {
        return findStockBrandOption(value)?.name || value;
    }
    if (key === "model") {
        const model = findStockModelOption(value);
        return model ? formatStockModelFilterOption(model) : value;
    }
    if (key === "locationName" && value === "__NO_LOCATION__") {
        return "Nie przyjęty";
    }
    if (key === "hasOffer" || key === "afterService") {
        return "TAK";
    }

    return value;
}

async function loadStock(page = 0, stateFilters = null) {
    page = normalizeStockPageIndex(page);
    const filters = normalizeStockFilters(stateFilters || getFilters());
    const requestId = ++stockLoadRequestId;

    abortCurrentStockLoad();

    const abortController = new AbortController();
    stockLoadAbortController = abortController;

    currentPage = page;
    persistStockListState(page, filters);

    try {
        renderFilterChips();

        const data = await fetchPhonesPage(page, filters, abortController.signal);
        if (requestId !== stockLoadRequestId) return;

        if (data.content.length === 0 && page > 0) {
            loadStock(page - 1, filters);
            return;
        }

        const phones = data.content.map(phone => ({
            technicalId: phone.technicalId,
            name: phone.name,
            model: phone.model,
            ram: phone.ram,
            memory: phone.memory,
            color: phone.color,
            simCardType: phone.simCardType,
            imei: phone.imei,
            sellingPrice: phone.sellingPrice,
            soldFor: phone.soldFor,
            purchasePrice: phone.purchasePrice,
            createDateTime: phone.createDateTime,
            status: phone.status,
            source: phone.source,
            locationName: phone.locationName,
            purchaseType: phone.purchaseType,
            comment: phone.comment,
            description: phone.description,
            batteryCondition: phone.batteryCondition,
            used: phone.used,
            isReserved: phone.isReserved,
            hasOffer: phone.hasOffer,
            afterService: phone.afterService,
            phoneModelTechnicalId: phone.phoneModelTechnicalId,
            phoneModelDisplayName: phone.phoneModelDisplayName
        }));


        renderPhones(phones);
        initDropdowns();
        M.Tooltip.init(document.querySelectorAll('.tooltipped'));
        startReservationTimers(phones);
        loadPagination(data.number, data.totalPages);
    } catch (error) {
        if (error.name === "AbortError") return;
        if (requestId !== stockLoadRequestId) return;

        console.error("Błąd podczas pobierania danych:", error);
        M.toast({html: 'Błąd ładowania danych z serwera', classes: 'red'});
    } finally {
        if (stockLoadAbortController === abortController) {
            stockLoadAbortController = null;
        }
    }

    if (requestId !== stockLoadRequestId) return;
    renderChips(filters);
}

async function fetchPhonesPage(page = 0, filters = {}, signal = null) {
    const params = new URLSearchParams();

    params.append("page", page);
    params.append("size", pageSize);

    Object.entries(filters).forEach(([key, value]) => {
        if (value !== null && value !== "") {
            params.append(key, value);
        }
    });

    const response = await fetch(`/api/v1/phones?${params.toString()}`, { signal });

    if (!response.ok) {
        throw new Error(`HTTP error! Status: ${response.status}`);
    }

    return await response.json();
}

function abortCurrentStockLoad() {
    if (!stockLoadAbortController) return;

    stockLoadAbortController.abort();
    stockLoadAbortController = null;
}

function loadPagination(currentPage, totalPages) {
    const pagination = document.getElementById("dynamicPagination");
    if (!pagination) return;
    pagination.innerHTML = "";

    function createPageItem({classes = "", inner = "", onClick = null}) {
        const li = document.createElement("li");
        if (classes) li.className = classes;

        const a = document.createElement("a");
        a.href = "#!";
        if (typeof inner === "string") a.innerHTML = inner;
        else if (inner instanceof Node) a.appendChild(inner);

        if (typeof onClick === "function") {
            a.addEventListener("click", (e) => {
                e.preventDefault();
                onClick();
            });
        } else {
            a.setAttribute("aria-disabled", "true");
            a.style.pointerEvents = "none";
        }

        li.appendChild(a);
        return li;
    }

    if (totalPages <= 0) return;

    // ========== PRZYCISK "POPRAWDNIE" ==========
    if (currentPage > 0) {
        const leftIcon = '<i class="material-icons">chevron_left</i>';
        pagination.appendChild(createPageItem({
            classes: "waves-effect",
            inner: leftIcon,
            onClick: () => loadStock(currentPage - 1)
        }));
    } else {
        // disabled
        const leftIcon = '<i class="material-icons">chevron_left</i>';
        pagination.appendChild(createPageItem({
            classes: "disabled",
            inner: leftIcon,
            onClick: null
        }));
    }

    // ========== LOGIKA WYŚWIETLANIA MAKS 7 STRON ==========
    const maxVisible = 7;
    let startPage = Math.max(0, currentPage - Math.floor(maxVisible / 2));
    let endPage = startPage + maxVisible - 1;

    if (endPage >= totalPages) {
        endPage = totalPages - 1;
        startPage = Math.max(0, endPage - maxVisible + 1);
    }

    // generuj numery stron
    for (let i = startPage; i <= endPage; i++) {
        if (i === currentPage) {
            pagination.appendChild(createPageItem({
                classes: "active blue-grey darken-2",
                inner: String(i + 1),
                onClick: null // nie klikalna, bo aktywna
            }));
        } else {
            pagination.appendChild(createPageItem({
                classes: "waves-effect",
                inner: String(i + 1),
                onClick: () => loadStock(i)
            }));
        }
    }

    // ========== PRZYCISK "NASTĘPNA" ==========
    if (currentPage < totalPages - 1) {
        const rightIcon = '<i class="material-icons">chevron_right</i>';
        pagination.appendChild(createPageItem({
            classes: "waves-effect",
            inner: rightIcon,
            onClick: () => loadStock(currentPage + 1)
        }));
    } else {
        const rightIcon = '<i class="material-icons">chevron_right</i>';
        pagination.appendChild(createPageItem({
            classes: "disabled",
            inner: rightIcon,
            onClick: null
        }));
    }
}

// Renderowanie kafli
const sellButtonIdPrefix = "sellBtn";
const editButtonIdPrefix = "editBtn";

const PURCHASE_TYPE_LABELS = {
    CASH: "Gotówka",
    VAT_EXEMPT: "Faktura marża (ZW)",
    VAT_23: "Faktura VAT (23%)"
};

function renderPhones(phones) {
    listContainer.innerHTML = "";
    phones.forEach((phone) => {
        const statusClass = phone.status ? phone.status.toUpperCase() : "";
        const inCart = isPhoneInCart(phone.technicalId);
        const displayPrice = getDisplayPrice(phone);

        const disableSellButton =
            phone.status !== "DOSTĘPNY"
            || !phone.locationName
            || inCart
                ? "disabled"
                : "";


        const card = `
      <div class="col s12">
        <div
          class="card z-depth-2"
          data-technical-id="${phone.technicalId}"
          data-name="${phone.name ?? ""}"
          data-model="${phone.model ?? ""}"
          data-color="${phone.color ?? ""}"
          data-sim-card-type="${phone.simCardType ?? ""}"
          data-memory="${phone.memory ?? ""}"
          data-ram="${phone.ram ?? ""}"
          data-source="${phone.source ?? ""}"
          data-imei="${phone.imei ?? ""}"
          data-description="${phone.description ?? ""}"
          data-comment="${phone.comment ?? ""}"
          data-used="${phone.used ?? false}"
          data-is-reserved="${phone.isReserved ?? false}"
          data-battery-condition="${phone.batteryCondition ?? ""}"
          data-selling-price="${phone.sellingPrice ?? ""}"
          data-sold-for="${phone.soldFor ?? ""}"
          data-purchase-price="${phone.purchasePrice ?? ""}"
          data-phone-model-technical-id="${phone.phoneModelTechnicalId ?? ""}"
          data-phone-model-display-name="${phone.phoneModelDisplayName ?? ""}"
        >
          <div class="card-content phone-card">
            <div class="phone-left">
                <div class="title-row">
                  <span class="card-title">${phone.name}</span>
                
                  <span class="condition-badge ${phone.used ? "USED" : "NEW"}">
                    ${phone.used ? "UŻYWANY" : "NOWY"}
                  </span>
                  ${phone.afterService ? `
                  <span class="condition-badge AFTER_SERVICE">
                    PO SERWISIE
                  </span>
                  ` : ""}
                  ${phone.hasOffer ? `
                  <span class="condition-badge pink lighten-2 white-text">
                    ONLINE
                  </span>
                  ` : ""}
                  ${phone.isReserved ? `
                  <span class="reservation-badge red lighten-2 white-text tooltipped" 
                        id="reservation-${phone.technicalId}"
                        data-position="top"
                        data-tooltip="Ładowanie informacji o rezerwacji...">
                    <i class="material-icons tiny">alarm</i>
                    <span class="reservation-text">REZERWACJA</span>
                    <span class="reservation-timer" id="timer-${phone.technicalId}">--:--</span>
                    <i class="material-icons tiny cancel-reservation" 
                       style="cursor: pointer; margin-left: 5px;"
                       data-id="${phone.technicalId}"
                       onclick="confirmCancelReservation('${phone.technicalId}')">close</i>
                  </span>
                  ` : ""}
                </div>
                <p><i class="material-icons tiny">smartphone</i> <b>Model:</b> ${phone.model}</p>

                <p>
                  <i class="material-icons tiny">dns</i>
                  <b>Model z bazy:</b> ${phone.phoneModelDisplayName || "Nie ustawiono"}
                </p>

                ${(phone.ram != null || phone.memory != null || phone.simCardType != null) ? `
                <p>
                  <i class="material-icons tiny">memory</i>
                  <b>Specyfikacja:</b>
                  ${phone.ram != null ? `${phone.ram}` : ``}
                  ${phone.ram != null && phone.memory != null ? ` / ` : ``}
                  ${phone.memory != null ? `${phone.memory}` : ``}
                  ${(phone.ram != null || phone.memory != null) && phone.simCardType != null ? ` / ` : ``}
                  ${phone.simCardType != null ? `${phone.simCardType}` : ``}
                </p>
                ` : ``}
                
                ${phone.source != null && phone.source !== "" ? `
                <p>
                  <i class="material-icons tiny">local_shipping</i>
                  <b>Pochodzenie:</b> ${phone.source}
                </p>
                ` : ``}

                <p><i class="material-icons tiny">palette</i> <b>Kolor:</b> ${phone.color}</p>
                
            <p>
              <i class="material-icons tiny">receipt</i>
              <b>Forma zakupu:</b>
              ${PURCHASE_TYPE_LABELS[phone.purchaseType] ?? "—"}

              ${
                PURCHASE_TYPE_LABELS[phone.purchaseType] !== "Gotówka"
                  ? `<i class="material-icons tiny orange-text tooltipped"
                       data-position="top"
                       data-tooltip="Zakup nie był gotówkowy">warning</i>`
                  : ""
              }
            </p>
                
                <p><i class="material-icons tiny">fingerprint</i> <b>IMEI:</b> ${phone.imei}</p>
                
                <p><i class="material-icons tiny">event</i> <b>Dodano:</b> ${phone.createDateTime}</p>
                
                ${phone.description? `<p> <i class="material-icons tiny">notes</i>  <b>Opis:</b> ${phone.description}</p>` : ``}
                
                ${phone.comment != null && phone.comment !== "" ? `
                <p>
                  <i class="material-icons tiny">comment</i>
                  <b>Komentarz:</b> ${phone.comment}
                </p>
                ` : ``}               
                ${phone.batteryCondition ? `
                <p class="battery-row">
                  <i class="material-icons tiny ${getBatteryIconClass(phone.batteryCondition)}">
                    ${getBatteryIcon(phone.batteryCondition)}
                  </i>
                  <b>Stan baterii:</b> ${phone.batteryCondition} %
                </p>
                ` : ``}
              ${
            phone.locationName
                ? `<p class="location">
               <i class="material-icons tiny blue-text">place</i>
               <b>${phone.locationName}</b>
             </p>`
                : `<p class="location grey-text">
               <i class="material-icons tiny">place</i>
               Brak lokalizacji
             </p>`
        }
            </div>
            <div class="phone-right">
            <div class="price-wrapper col s12">
                <p class="sellingPrice">${formatPrice(displayPrice)} zł</p>
                <p class="initialPrice-hover">Zakup: ${phone.purchasePrice} zł</p>
            </div>          
              <span class="status-badge ${statusClass}">${phone.status}</span>
            </div>
          </div>
         <div class="card-action right-align">  
              <a class='dropdown-trigger btn-small blue darken-2' href='#' data-target='dropdown-${phone.technicalId}'>
                <i class="material-icons left">more_vert</i>Więcej
              </a>
            
            <ul id="dropdown-${phone.technicalId}" class="dropdown-content">
            
              <li>
                <a href="#!" data-action="edit" data-id="${phone.technicalId}">
                  Edytuj
                </a>
              </li>

              <li>
                <a href="#!" data-action="assign-model" data-id="${phone.technicalId}">
                  Ustaw model z bazy
                </a>
              </li>
            
              <li>
                <a href="#!"
                   data-action="accept"
                   data-id="${phone.technicalId}"
                   class="${phone.status !== 'WPROWADZONY' ? 'disabled-link' : ''}">
                  Przyjmij na sklep
                </a>
              </li>
            
             <li>
              <a href="#!"
                 data-action="remove-from-location"
                 data-id="${phone.technicalId}"
                 class="${phone.locationName && phone.status === 'DOSTĘPNY'
            ? ''
            : 'disabled-link'}">
                Usuń z lokalizacji
              </a>
            </li>
            <li>
              <a href="#!"
                 data-action="handover"
                 data-id="${phone.technicalId}"
                 class="${phone.status !== 'DOSTĘPNY' ? 'disabled-link' : ''}">
                Przekaż
              </a>
            </li>
              <li class="red-text">
                <a href="#!"
                   data-action="delete"
                   data-id="${phone.technicalId}"
                   class="${!['WPROWADZONY', 'DOSTĘPNY'].includes(phone.status)
            ? 'disabled-link'
            : ''}">
                  Usuń
                </a>
              </li>

              ${IS_ADMIN && phone.status === 'USUNIĘTY' ? `
              <li>
                <a href="#!" data-action="restore" data-id="${phone.technicalId}">
                  Przywróć
                </a>
              </li>
              ` : ''}

              ${!phone.isReserved && phone.status === 'DOSTĘPNY' ? `
              <li>
                <a href="#!" data-action="reserve" data-id="${phone.technicalId}">
                  Zarezerwuj
                </a>
              </li>
              ` : ''}
            
            </ul>
            
              <button class="btn-small green darken-2 ${disableSellButton}"
                 onclick="sellPhone('${phone.technicalId}')"
                ${disableSellButton ? "disabled" : ""}>
                <i class="material-icons left">attach_money</i>
                ${inCart ? "W koszyku" : "Sprzedaj"}
              </button>

        </div>
        </div>
      </div>
    `;

        const sellButtonId = sellButtonIdPrefix + phone.technicalId;
        listContainer.innerHTML += card;
    });

    M.Dropdown.init(document.querySelectorAll('.dropdown-trigger'), {
        closeOnClick: false,
        constrainWidth: false,
        coverTrigger: false
    });

}

function getDisplayPrice(phone) {
    if (phone.status === "SPRZEDANY" && phone.soldFor != null) {
        return phone.soldFor;
    }

    return phone.sellingPrice;
}

function formatPrice(price) {
    if (price == null || price === "") {
        return "—";
    }

    const value = Number(price);
    return Number.isNaN(value) ? price : value
}

let cartBtn = null;

function startReservationTimers(phones) {
    phones.filter(p => p.isReserved).forEach(async (phone) => {
        try {
            const response = await fetch(`/api/v1/external/reservations/status/${phone.technicalId}`);
            if (!response.ok) return;
            const status = await response.json();
            
            const badge = document.getElementById(`reservation-${phone.technicalId}`);
            if (badge) {
                const tooltipInstance = M.Tooltip.getInstance(badge);
                if (tooltipInstance) {
                    tooltipInstance.destroy();
                }
                badge.setAttribute('data-tooltip', `Rezerwujący: ${status.name} (${status.phoneNumber})`);
                M.Tooltip.init(badge);
            }

            const expiryTime = new Date(status.expiryTime).getTime();
            const timerElement = document.getElementById(`timer-${phone.technicalId}`);
            
            if (timerElement) {
                const interval = setInterval(() => {
                    const now = new Date().getTime();
                    const distance = expiryTime - now;
                    
                    if (distance < 0) {
                        clearInterval(interval);
                        timerElement.innerHTML = "EXPIRED";
                        return;
                    }
                    
                    const hours = Math.floor((distance % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
                    const minutes = Math.floor((distance % (1000 * 60 * 60)) / (1000 * 60));
                    const seconds = Math.floor((distance % (1000 * 60)) / 1000);
                    
                    timerElement.innerHTML = `${hours}h ${minutes}m ${seconds}s`;
                }, 1000);
            }
        } catch (e) {
            console.error("Error loading reservation status", e);
        }
    });
}

function initDropdowns() {
    M.Modal.init(document.querySelectorAll('.modal'));
    const elems = document.querySelectorAll('.dropdown-trigger');
    M.Dropdown.init(elems, {
        constrainWidth: false,
        coverTrigger: false,
        alignment: 'right'
    });
}

function acceptPhone(technicalId) {
    const card = document.querySelector(`.card[data-technical-id="${technicalId}"]`);
    if (!card) return;

    const status = card.querySelector(".status-badge")?.textContent.trim();

    if (status !== "WPROWADZONY") {
        M.toast({html: "Telefon nie może zostać przyjęty na sklep", classes: "red"});
        return;
    }

    const confirmBtn = document.getElementById("confirmAcceptYes");
    confirmBtn.setAttribute("data-technical-id", technicalId);

    // otwórz modal
    const modal = M.Modal.getInstance(document.getElementById("acceptConfirmModal"));
    modal.open();
}


async function sellPhone(technicalId) {
    if (isPhoneInCart(technicalId)) {
        return;
    }

    const btn = document.getElementById("sellBtn" + technicalId);
    if (btn) {
        btn.disabled = true;
        btn.classList.add("grey");
        btn.innerText = "W koszyku";
    }

    try {
        // opcjonalnie: zablokuj przycisk na czas requestu
        const btn = document.querySelector(`button[data-technical-id="${technicalId}"]`);
        if (btn) {
            btn.disabled = true;
        }

        const params = new URLSearchParams({technicalId});

        const response = await fetch(`/api/v1/cart/add?${params.toString()}`, {
            method: 'POST',
            headers: {
                'X-Requested-With': 'XMLHttpRequest'
                // jeśli używasz CSRF, trzeba tu dodać nagłówek z tokenem
            }
        });

        if (response.status === 409) {
            M.toast({ html: 'Telefon nie jest w twojej lokalizacji', classes: 'red' });

            // cofamy UI
            const btn = document.getElementById("sellBtn" + technicalId);
            if (btn) {
                btn.disabled = false;
                btn.innerText = "Sprzedaj";
                btn.classList.remove("grey");
            }
            return;
        }


        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }


        const updatedCart = await response.json();
        cartState = updatedCart;
        refreshStockListPreservingState();

        // 🔢 aktualizacja liczby w koszyku – dostosuj do swojego modelu Cart
        // Zakładam np. że masz: cart.items albo cart.phones
        const items = updatedCart.items || updatedCart.phones || [];
        cartCount = items.length;

        // Jeśli masz gdzieś licznik w UI, np. badge przy ikonce koszyka:
        const cartCountBadge = document.getElementById('cartCountBadge');
        if (cartCountBadge) {
            cartCountBadge.textContent = cartCount;
        }

        M.toast({html: 'Telefon dodany do koszyka', classes: 'green'});

        // opcjonalnie: zmień wygląd przycisku na „W koszyku”
        if (btn) {
            btn.textContent = 'W koszyku';
            btn.classList.remove('orange', 'darken-2');
            btn.classList.add('grey');
        }

    } catch (e) {
        console.error('Error adding to cart:', e);
        M.toast({html: 'Nie udało się dodać do koszyka', classes: 'red'});

        // przy błędzie pozwól znowu kliknąć
        const btn = document.querySelector(`button[data-technical-id="${technicalId}"]`);
        if (btn) {
            btn.disabled = false;
        }
    }
}

document.addEventListener('DOMContentLoaded', async () => {
    try {
        const res = await fetch('/api/v1/cart', {headers: {'X-Requested-With': 'XMLHttpRequest'}});
        if (res.ok) {
            const cart = await res.json();
            cartState = cart;
            const items = cart.items || cart.phones || [];
            cartCount = items.length;

            const cartCountBadge = document.getElementById('cartCountBadge');
            if (cartCountBadge) {
                cartCountBadge.textContent = cartCount;
            }
        }
    } catch (e) {
        console.warn('Nie udało się pobrać koszyka przy starcie:', e);
    }
});

function createCartButton() {
    cartBtn = document.createElement('div');
    cartBtn.id = 'cartBtn';
    cartBtn.className = 'btn-floating btn-large blue';
    cartBtn.innerHTML = `
        <i class="material-icons">shopping_cart</i>
        <span id="cartBadge" class="badge">0</span>
    `;

    document.body.appendChild(cartBtn);
}

document.addEventListener("DOMContentLoaded", async function () {
    // Inicjalizacja modali
    const dropArea = document.getElementById('dropArea');
    const fileInput = document.getElementById('fileInput');

    // Kliknięcie na dropArea otwiera wybór plików
    dropArea.addEventListener('click', () => fileInput.click());

    // Obsługa przeciągania plików
    dropArea.addEventListener('dragover', (e) => {
        e.preventDefault();
        dropArea.classList.add('dragover');
    });

    dropArea.addEventListener('dragleave', () => dropArea.classList.remove('dragover'));
    dropArea.addEventListener('drop', (e) => {
        e.preventDefault();
        dropArea.classList.remove('dragover');
        const files = e.dataTransfer.files;
        handleFiles(files);
    });

    // Obsługa wybranych plików z dialogu
    fileInput.addEventListener('change', () => handleFiles(fileInput.files));

    function handleFiles(files) {
        console.log("Wybrane pliki:", files);
        M.toast({html: `${files.length} plików wybranych`, classes: 'green'});
        // Tutaj później będzie logika przesyłania do backendu
    }

    createCartButton();
    [
        ...stockFilterFieldIds,
        ...stockBooleanFilterIds
    ].forEach(id => {
        const el = document.getElementById(id);
        if (!el) return;
        el.addEventListener("input", liveReload);
        el.addEventListener("change", liveReload);
    });

    [
        "filterStatus",
        "filterLocation",
        "filterBrand",
        "filterModel",
        "filterMemory"
    ].forEach(id => {
        const el = document.getElementById(id);
        if (!el) return;
        el.addEventListener("change", () => {
            if (!getTomSelectInstance(el)) {
                liveReload();
            }
        });
    });

    stockStatusSelect = initSimpleSelect('#filterStatus');
    stockLocationSelect = initSimpleSelect('#filterLocation');
    initStockModelFilterSelects();
    assignPhoneModelSelect = initPhoneModelSelect('#assignPhoneModelSelect');
    assignPhoneModelSelect?.on('change', value => {
        loadAssignModelVariants(value, currentAssignModelCard());
    });

    if (stockStatusSelect) {
        stockStatusSelect.on('change', () => {
            if (!isRestoringStockListState) liveReload();
        });
    }
    if (stockLocationSelect) {
        stockLocationSelect.on('change', () => {
            if (!isRestoringStockListState) liveReload();
        });
    }

    await loadStockModelFilterOptions();
    await loadLocationsFilter();
    loadStock(restoreStockListState());
});

function editPhone(technicalId) {
    try {
        const card = document.querySelector(
            `.card[data-technical-id="${technicalId}"]`
        );
        if (!card) return;

        document.getElementById("editName").value =
            card.dataset.name || "";

        document.getElementById("editModel").value =
            card.dataset.model || "";

        document.getElementById("editColor").value =
            card.dataset.color || "";

        document.getElementById("editImei").value =
            card.dataset.imei || "";

        document.getElementById("editSource").value =
            card.dataset.source || "";

        document.getElementById("editPrice").value =
            card.dataset.sellingPrice || "";

        document.getElementById("editDescription").value =
            card.dataset.description || "";

        document.getElementById("editComment").value =
            card.dataset.comment || "";

        document.getElementById("editUsed").checked =
            card.dataset.used === "true";

        document.getElementById("editBatteryCondition").value =
            card.dataset.batteryCondition || "";

        // 🔐 ADMIN ONLY – purchasePrice
        if (IS_ADMIN) {
            document.getElementById("editPurchasePrice").value =
                card.dataset.purchasePrice || "";
        }

        M.updateTextFields();
        updateEditBatteryVisibility();

        document
            .getElementById("saveEditBtn")
            .setAttribute("data-technical-id", technicalId);

        M.Modal.getInstance(
            document.getElementById("editPhoneModal")
        ).open();

    } catch (e) {
        console.error(e);
        M.toast({ html: "Błąd edycji telefonu", classes: "red" });
    }
}

["editName", "editModel", "editUsed"].forEach(id => {
    document.getElementById(id).addEventListener("input", updateEditBatteryVisibility);
    document.getElementById(id).addEventListener("change", updateEditBatteryVisibility);
});


function shouldShowBatteryCondition(name, model, used) {
    const text = `${name} ${model}`.toLowerCase();
    return used === true && text.includes("iphone");
}

function updateEditBatteryVisibility() {
    const name = document.getElementById("editName").value || "";
    const model = document.getElementById("editModel").value || "";
    const used = document.getElementById("editUsed").checked;

    const wrapper = document.getElementById("editBatteryConditionWrapper");
    const input = document.getElementById("editBatteryCondition");

    if (shouldShowBatteryCondition(name, model, used)) {
        wrapper.classList.remove("hide");
    } else {
        wrapper.classList.add("hide");
        input.value = "";
    }

    M.updateTextFields();
}


document.getElementById("saveEditBtn").addEventListener("click", async () => {
    const technicalId =
        document.getElementById("saveEditBtn")
            .getAttribute("data-technical-id");

    const name = document.getElementById("editName").value.trim();
    const model = document.getElementById("editModel").value.trim();
    const used = document.getElementById("editUsed").checked;

    const batteryInput = document.getElementById("editBatteryCondition");
    const shouldSendBattery =
        shouldShowBatteryCondition(name, model, used);

    const updatedPhone = {
        name: name,
        model: model,
        color: document.getElementById("editColor").value.trim(),
        imei: document.getElementById("editImei").value.trim(),
        source: document.getElementById("editSource").value.trim(),
        sellingPrice: parseFloat(
            document.getElementById("editPrice").value
        ),
        description: document.getElementById("editDescription").value.trim(),
        comment: document.getElementById("editComment").value.trim(),
        used: used,
        purchasePrice: document.getElementById("editPurchasePrice").value,
        batteryCondition:
            shouldSendBattery && batteryInput.value !== ""
                ? batteryInput.value
                : null
    };

    console.log("PATCH payload:", updatedPhone);

    try {
        const response = await fetch(`/api/v1/phones/${technicalId}`, {
            method: "PATCH",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(updatedPhone)
        });

        if (!response.ok) {
            throw new Error("Nie udało się zapisać zmian");
        }

        M.toast({
            html: "Telefon został zaktualizowany",
            classes: "green"
        });

        const modal = M.Modal.getInstance(
            document.getElementById("editPhoneModal")
        );
        modal.close();

        refreshStockListPreservingState();

    } catch (error) {
        console.error("Błąd podczas zapisu:", error);
        M.toast({ html: "Błąd podczas zapisu", classes: "red" });
    }
});

document.getElementById("resetFilters").addEventListener("click", () => {
    stockFilterFieldIds.forEach(id => {
        const el = document.getElementById(id);
        if (el) el.value = "";
    });
    stockBooleanFilterIds.forEach(id => {
        const el = document.getElementById(id);
        if (el) el.checked = false;
    });
    M.updateTextFields();

    const statusSelect = getTomSelectInstance('#filterStatus');
    const locationSelect = getTomSelectInstance('#filterLocation');
    const brandSelect = getTomSelectInstance('#filterBrand');
    const modelSelect = getTomSelectInstance('#filterModel');
    const memorySelect = getTomSelectInstance('#filterMemory');

    statusSelect?.clear(true);
    locationSelect?.clear(true);
    brandSelect?.clear(true);
    updateStockModelOptions("", "");
    modelSelect?.clear(true);
    memorySelect?.clear(true);

    renderFilterChips();
    loadStock(0);
});


// tłumaczenia etykiet
const filterLabels = {
    name: "Nazwa",
    brand: "Marka",
    model: "Model",
    memory: "Pamięć",
    color: "Kolor",
    imei: "IMEI",
    priceMin: "Cena od",
    priceMax: "Cena do",
    status: "Status",
    locationName: "Lokalizacja",
    hasOffer: "Oferta",
    afterService: "Po serwisie"
};

// === CHIPS SECTION === //
const chipContainer = document.getElementById("activeChips");

// generowanie chipów
function renderChips(filters) {

    if (!chipContainer) return;

    chipContainer.innerHTML = "";

    Object.entries(filters).forEach(([key, val]) => {
        if (!val) return;

        // 👇 mapowanie technicznej wartości na nazwę dla UI
        let displayValue = getStockFilterDisplayValue(key, val);

        const chip = document.createElement("div");
        chip.className = "chip";

        chip.innerHTML = `
            ${filterLabels[key]}: <b>${displayValue}</b>
            <i class="close material-icons" data-filter="${key}">close</i>
        `;

        chipContainer.appendChild(chip);
    });

    // usuwanie chipa → czyści filtr → reload
    chipContainer.querySelectorAll(".close").forEach(icon => {
        icon.addEventListener("click", () => {
            const filterKey = icon.dataset.filter;

            const inputId = "filter" + capitalize(filterKey);
            const el = document.getElementById(inputId);

            if (el) {
                if (el.type === "checkbox") {
                    el.checked = false;
                } else if (el.tagName === "SELECT") {
                    getTomSelectInstance(el)?.clear(true);
                } else {
                    el.value = "";
                }
            }

            if (filterKey === "brand") {
                updateStockModelOptions("", "");
            }

            M.updateTextFields();
            loadStock(0);
        });
    });
}

function capitalize(str) {
    return str.charAt(0).toUpperCase() + str.slice(1);
}


function renderFilterChips() {
    const filters = getFilters();
    const container = document.getElementById("activeFiltersChips");
    container.innerHTML = "";

    // jawne mapowanie key → inputId
    const filterInputMap = {
        name: "filterName",
        brand: "filterBrand",
        model: "filterModel",
        memory: "filterMemory",
        color: "filterColor",
        imei: "filterImei",
        priceMin: "filterPriceMin",
        priceMax: "filterPriceMax",
        status: "filterStatus",
        locationName: "filterLocation",
        hasOffer: "filterHasOffer",
        afterService: "filterAfterService"
    };

    Object.entries(filters).forEach(([key, value]) => {
        if (!value) return;

        let displayValue = getStockFilterDisplayValue(key, value);

        const chip = document.createElement("div");
        chip.className = "chip";
        chip.innerHTML = `
            ${filterLabels[key]}: ${displayValue}
            <i class="close material-icons">close</i>
        `;

        chip.querySelector("i").addEventListener("click", () => {
            const inputId = filterInputMap[key];
            const el = document.getElementById(inputId);

            if (el) {
                if (el.type === "checkbox") {
                    el.checked = false;
                } else if (el.tagName === "SELECT") {
                    getTomSelectInstance(el)?.clear(true);
                } else {
                    el.value = "";
                }
            }

            if (key === "brand") {
                updateStockModelOptions("", "");
            }

            M.updateTextFields();
            renderFilterChips();
            loadStock(0);
        });

        container.appendChild(chip);
    });
}

// Kliknięcie "TAK" w potwierdzeniu
document.getElementById("confirmAcceptYes").addEventListener("click", async () => {
    const technicalId = document
        .getElementById("confirmAcceptYes")
        .getAttribute("data-technical-id");

    try {
        const response = await fetch(`/api/v1/phones/${technicalId}/accept`, {
            method: "POST"
        });

        if (!response.ok) {
            throw new Error("Błąd podczas przyjmowania na sklep");
        }

        M.toast({html: "Telefon przyjęty na sklep", classes: "green"});

        // Zamknij modale
        M.Modal.getInstance(document.getElementById("acceptConfirmModal")).close();

        // Odśwież listę
        refreshStockListPreservingState();

    } catch (error) {
        console.error(error);
        M.toast({html: "Nie udało się przyjąć telefonu", classes: "red"});
    }
});

async function loadLocationsFilter() {
    try {
        const res = await fetch("/api/v1/locations");
        if (!res.ok) return;

        const locations = await res.json();
        const select = document.getElementById("filterLocation");

        if (!select || !select.tomselect) {
            console.warn("Tom Select nie jest jeszcze zainicjalizowany");
            return;
        }

        const ts = select.tomselect;

        // czyścimy opcje (tylko dynamiczne)
        ts.clearOptions();

        // opcje bazowe
        ts.addOption({ value: "", text: "Dowolna" });
        ts.addOption({ value: "__NO_LOCATION__", text: "Brak lokalizacji" });

        // opcje z backendu
        locations.forEach(loc => {
            ts.addOption({
                value: loc.name,
                text: loc.name
            });
        });

        ts.refreshOptions(false);

    } catch (e) {
        console.warn("Nie udało się pobrać lokalizacji", e);
    }
}

function isPhoneInCart(technicalId) {
    if (!cartState || !cartState.items) return false;

    return cartState.items.some(item =>
        item.technicalId === technicalId && item.itemType === "PHONE"
    );
}

function debounce(fn, delay = 300) {
    let timeout;
    return (...args) => {
        clearTimeout(timeout);
        timeout = setTimeout(() => fn(...args), delay);
    };
}



const debouncedStockReload = debounce(() => loadStock(0), 300);

function liveReload() {
    if (isRestoringStockListState) return;

    stockLoadRequestId++;
    abortCurrentStockLoad();
    currentPage = 0;
    persistStockListState(0, getFilters());
    debouncedStockReload();
}


let phoneToDelete = null;

function confirmDeletePhone(technicalId) {
    phoneToDelete = technicalId;
    const modal = M.Modal.getInstance(
        document.getElementById("deletePhoneModal")
    );
    modal.open();
}

document.getElementById("confirmDeletePhoneBtn")
    .addEventListener("click", () => {
        fetch(`/api/v1/phones/${phoneToDelete}`, {
            method: "DELETE"
        })
            .then(resp => {
                if (!resp.ok) {
                    console.log("DELETE resp not ok: " + resp)
                    return resp.text().then(t => { throw new Error(t); });
                }

                M.toast({
                    html: "Telefon oznaczony jako usunięty",
                    classes: "green"
                });

                refreshStockListPreservingState();
            })
            .catch(err => {
                M.toast({
                    html: "Nie można usunąć telefonu",
                    classes: "red"
                });
            })
            .finally(() => {
                M.Modal.getInstance(
                    document.getElementById("deletePhoneModal")
                ).close();
            })
    });

document.addEventListener("touchstart", handleDropdownAction, { passive: false });
document.addEventListener("click", handleDropdownAction);


function handleDropdownAction(e) {
    const el = e.target.closest("a[data-action]");
    if (!el) return;

    e.preventDefault();
    e.stopImmediatePropagation(); // 🔥 WAŻNE

    const action = el.dataset.action;
    const id = el.dataset.id;

    // ręcznie zamykamy dropdown
    const dropdown = el.closest(".dropdown-content");
    const instance = M.Dropdown.getInstance(
        document.querySelector(`[data-target="${dropdown.id}"]`)
    );
    if (instance) instance.close();

    switch (action) {
        case "edit":
            editPhone(id);
            break;

        case "assign-model":
            openAssignModelModal(id);
            break;

        case "accept":
            acceptPhone(id);
            break;

        case "remove-from-location":
            removePhoneFromLocation(id);
            break;

        case "delete":
            confirmDeletePhone(id);
            break;

        case "restore":
            restorePhone(id);
            break;

        case "handover":
            openHandoverModal(id);
            break;

        case "reserve":
            openReservationModal(id);
            break;

    }
}

async function openAssignModelModal(technicalId) {
    phoneToAssignModel = technicalId;
    const card = document.querySelector(`.card[data-technical-id="${technicalId}"]`);
    const phoneInfo = document.getElementById("assignModelPhoneInfo");
    const currentModelId = card?.dataset.phoneModelTechnicalId || "";
    const currentModelDisplayName = card?.dataset.phoneModelDisplayName || "";

    phoneInfo.textContent = `${card?.dataset.name || ""} ${card?.dataset.model || ""}`.trim();

    if (!assignPhoneModelSelect) {
        assignPhoneModelSelect = initPhoneModelSelect('#assignPhoneModelSelect');
    }

    await loadAssignPhoneModelOptions(currentModelId, currentModelDisplayName);
    await loadAssignModelVariants(currentModelId, card);

    M.Modal.getInstance(document.getElementById("assignModelModal")).open();
}

document.getElementById("saveAssignModelBtn")
    .addEventListener("click", async () => {
        const phoneModelTechnicalId = assignPhoneModelSelect?.getValue();

        if (!phoneToAssignModel || !phoneModelTechnicalId) {
            M.toast({ html: "Wybierz model z bazy", classes: "red" });
            return;
        }

        if (!assignRamSelect || !assignMemorySelect || !assignColorSelect || !assignSimCardTypeSelect) {
            M.toast({ html: "Poczekaj na załadowanie opcji RAM, pamięci, koloru i SIM", classes: "red" });
            return;
        }

        try {
            const card = document.querySelector(`.card[data-technical-id="${phoneToAssignModel}"]`);
            const response = await fetch(`/api/v1/phones/${phoneToAssignModel}`, {
                method: "PATCH",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    name: card?.dataset.name || null,
                    model: card?.dataset.model || null,
                    color: assignColorSelect?.getValue() || null,
                    simCardType: assignSimCardTypeSelect?.getValue() || null,
                    imei: card?.dataset.imei || null,
                    source: card?.dataset.source || null,
                    sellingPrice: card?.dataset.sellingPrice || null,
                    purchasePrice: card?.dataset.purchasePrice || null,
                    description: card?.dataset.description || null,
                    comment: card?.dataset.comment || null,
                    used: card?.dataset.used === "true",
                    batteryCondition: card?.dataset.batteryCondition || null,
                    ram: assignRamSelect?.getValue() || null,
                    memory: assignMemorySelect?.getValue() || null,
                    phoneModelTechnicalId
                })
            });

            if (!response.ok) {
                throw new Error("Nie udało się ustawić modelu");
            }

            M.toast({ html: "Model z bazy został ustawiony", classes: "green" });
            M.Modal.getInstance(document.getElementById("assignModelModal")).close();
            refreshStockListPreservingState();
        } catch (e) {
            M.toast({ html: "Błąd ustawiania modelu", classes: "red" });
        }
    });

function openReservationModal(technicalId) {
    phoneToReserve = technicalId;
    document.getElementById("reservationName").value = "";
    document.getElementById("reservationPhone").value = "";
    M.updateTextFields();
    M.Modal.getInstance(document.getElementById("reservationModal")).open();
}

function openHandoverModal(technicalId) {
    phoneToHandover = technicalId;

    document.getElementById("handoverComment").value = "";
    document.getElementById("handoverPrice").value = "";
    M.updateTextFields();

    const modal = M.Modal.getInstance(
        document.getElementById("handoverPhoneModal")
    );
    modal.open();
}


function removePhoneFromLocation(technicalId) {
    fetch(`/api/v1/phones/${technicalId}/remove-from-location`, {
        method: "POST"
    })
        .then(res => {
            if (!res.ok) throw new Error("Błąd usuwania z lokalizacji");
            M.toast({ html: "Telefon usunięty z lokalizacji", classes: "green" });
            return refreshStockListPreservingState();
        })
        .catch(err => {
            M.toast({ html: err.message });
        });
}

function restorePhone(technicalId) {
    fetch(`/api/v1/phones/${technicalId}/restore`, {
        method: "POST"
    })
        .then(res => {
            if (!res.ok) throw new Error("Błąd przywracania telefonu");
            M.toast({ html: "Telefon został przywrócony", classes: "green" });
            return refreshStockListPreservingState();
        })
        .catch(err => {
            M.toast({ html: err.message, classes: "red" });
        });
}

document.getElementById("confirmHandoverBtn")
    .addEventListener("click", async () => {

        const comment =
            document.getElementById("handoverComment").value;

        const price =
            document.getElementById("handoverPrice").value;

        try {
            const response = await fetch(
                `/api/v1/phones/${phoneToHandover}/handover`,
                {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({ comment, price })
                }
            );

            if (!response.ok) {
                throw new Error("Błąd przekazania");
            }

            M.toast({ html: "Telefon przekazany", classes: "green" });

            const modal = M.Modal.getInstance(
                document.getElementById("handoverPhoneModal")
            );
            modal.close();

            refreshStockListPreservingState();

        } catch (err) {
            console.error(err);
            M.toast({ html: "Błąd przekazania", classes: "red" });
        }
    });

document.getElementById("confirmReservationBtn")
    .addEventListener("click", async () => {
        const name = document.getElementById("reservationName").value;
        const phoneNumber = document.getElementById("reservationPhone").value;

        if (!name || !phoneNumber) {
            M.toast({ html: "Wypełnij wszystkie pola", classes: "orange" });
            return;
        }

        try {
            const response = await fetch("/api/v1/external/reservations", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    name,
                    phoneNumber,
                    technicalId: phoneToReserve
                })
            });

            if (response.ok) {
                M.toast({ html: "Zarezerwowano pomyślnie", classes: "green" });
                refreshStockListPreservingState();
            } else {
                const err = await response.text();
                M.toast({ html: `Błąd: ${err}`, classes: "red" });
            }
        } catch (e) {
            M.toast({ html: "Błąd połączenia", classes: "red" });
        } finally {
            M.Modal.getInstance(document.getElementById("reservationModal")).close();
        }
    });


function initSimpleSelect(selector) {
    const el = document.querySelector(selector);
    if (!el) return null;

    return new TomSelect(el, {
        controlInput: null,
        create: false,
        searchField: [],
        shouldSort: false,
        hideSelected: true,
        closeAfterSelect: true,
        allowEmptyOption: true
    });
}

function confirmCancelReservation(technicalId) {
    if (confirm("Czy na pewno chcesz usunąć rezerwację?")) {
        cancelReservation(technicalId);
    }
}

async function cancelReservation(technicalId) {
    try {
        const response = await fetch(`/api/v1/reservations/${technicalId}`, {
            method: 'DELETE'
        });
        if (response.ok) {
            M.toast({ html: "Rezerwacja została usunięta", classes: "green" });
            refreshStockListPreservingState();
        } else {
            const err = await response.text();
            M.toast({ html: `Błąd: ${err}`, classes: "red" });
        }
    } catch (e) {
        M.toast({ html: "Błąd połączenia", classes: "red" });
    }
}

function getBatteryIcon(percent) {
    const p = Number(percent);

    if (p >= 90) return "battery_full";
    if (p >= 80) return "battery_6_bar";
    if (p >= 70) return "battery_5_bar";
    if (p >= 60) return "battery_4_bar";
    if (p >= 50) return "battery_3_bar";
    if (p >= 30) return "battery_2_bar";
    if (p >= 15) return "battery_1_bar";

    return "battery_alert";
}

function getBatteryIconClass(percent) {
    const p = Number(percent);

    if (p >= 80) return "battery-good";
    if (p >= 50) return "battery-ok";
    if (p >= 30) return "battery-warn";

    return "battery-bad";
}
