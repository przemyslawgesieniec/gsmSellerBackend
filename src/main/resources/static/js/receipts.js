    let currentPage = 0;
    const pageSize = 10;
    let dateRangePicker = null;

    document.addEventListener("DOMContentLoaded", () => {
        initFilters();
        loadReceipts();

        document.getElementById("loadMoreBtn").addEventListener("click", () => {
            currentPage++;
            loadReceipts();
        });
    });

    function initFilters() {
        dateRangePicker = flatpickr("#receiptDateRange", {
            mode: "range",
            dateFormat: "Y-m-d",
            locale: flatpickr.l10ns.pl,
            monthSelectorType: "dropdown",
            onClose: function(selectedDates) {
                if (selectedDates.length === 2 || selectedDates.length === 0) {
                    resetAndLoadReceipts();
                }
            }
        });

        ["invoiceNumberFilter", "receiptImeiFilter"].forEach(id => {
            document.getElementById(id)?.addEventListener("input", debounce(resetAndLoadReceipts, 300));
        });

        document.getElementById("clearReceiptFiltersBtn")?.addEventListener("click", () => {
            document.getElementById("invoiceNumberFilter").value = "";
            document.getElementById("receiptImeiFilter").value = "";
            if (dateRangePicker) dateRangePicker.clear();
            M.updateTextFields();
            resetAndLoadReceipts();
        });
    }

    function resetAndLoadReceipts() {
        currentPage = 0;
        loadReceipts();
    }

    function loadReceipts() {
        fetch(buildReceiptsUrl())
            .then(resp => resp.json())
            .then(data => renderReceipts(data))
            .catch(err => {
                console.error("Błąd pobierania faktur", err);
                M.toast({html: "Błąd pobierania faktur", classes: "red"});
            });
    }

    function buildReceiptsUrl() {
        const params = new URLSearchParams({
            page: currentPage,
            size: pageSize
        });

        const invoiceNumber = document.getElementById("invoiceNumberFilter")?.value.trim();
        const imei = document.getElementById("receiptImeiFilter")?.value.trim();

        if (invoiceNumber) params.append("invoiceNumber", invoiceNumber);
        if (imei) params.append("imei", imei);

        if (dateRangePicker && dateRangePicker.selectedDates.length === 2) {
            params.append("dateFrom", formatDateOnly(dateRangePicker.selectedDates[0]));
            params.append("dateTo", formatDateOnly(dateRangePicker.selectedDates[1]));
        }

        return `/api/v1/receipt/list?${params.toString()}`;
    }

    function renderReceipts(pageData) {
        const list = document.getElementById("receiptList");

        if (currentPage === 0) {
            list.innerHTML = "";
        }

        if (!pageData.content.length && currentPage === 0) {
            list.innerHTML = '<li class="center-align grey-text" style="padding: 20px;">Brak faktur spełniających kryteria</li>';
        }

        pageData.content.forEach(receipt => {

            const li = document.createElement("li");
            const statusBadge = receipt.status === "WYCOFANA"
                ? `<span class="new badge red" data-badge-caption="WYCOFANA"></span>`
                : "";
            const sellDate = receipt.dateAndPlace?.sellDate;
            const sellDateLabel = sellDate ? formatDateOnlyString(sellDate) : "brak";

            const header = `
                <div class="collapsible-header"
                     style="display:flex; justify-content:space-between; align-items:center;">
                    <span>
                        <strong>${escapeHtml(receipt.number)}</strong>
                        ${statusBadge}<br>
                        <small>Sprzedaż: ${sellDateLabel} | Wystawiono: ${formatDate(receipt.createDate)}</small>
                    </span>
                
                    <button class="btn small blue"
                        onclick="downloadPdf('${receipt.technicalId}')">
                        Pobierz PDF
                    </button>
                </div>
            `;

            //  RENDEROWANIE POZYCJI FAKTURY ZGODNIE Z JSON
            const VAT_MAP = {
                VAT_EXEMPT: "ZW"
            };

            const body = `
    <div class="collapsible-body">
        <h6>Pozycje dokumentu:</h6>

        <ul class="collection">
            ${receipt.items.map(item => {
                const isVatNumeric = typeof item.vatRate === "number";

                const vatLabel = isVatNumeric
                    ? `${(item.vatRate * 100).toFixed(0)}% (${item.vatAmount.toFixed(2)} zł)`
                    : (VAT_MAP[item.vatRate] || item.vatRate);

                return `
                    <li class="collection-item">
                        <div><strong>${escapeHtml(item.name)}</strong></div>

                        <div class="grey-text">
                            Typ: ${escapeHtml(item.itemType)} <br>
                            Netto: ${item.nettAmount.toFixed(2)} zł <br>
                            VAT: ${vatLabel} <br>
                            Brutto: <strong>${item.grossAmount.toFixed(2)} zł</strong>
                        </div>
                    </li>
                `;
            }).join("")}
        </ul>

       <div style="margin-top:15px; display:flex; gap:10px;">
            <button class="btn green"
                onclick="downloadServiceInvoice('${receipt.technicalId}')">
                Pobierz fakturę serwisową
            </button>
        
            ${receipt.status === "AKTYWNA" ? `
                <button class="btn red"
                    onclick="confirmCancelReceipt('${receipt.technicalId}')">
                    Anuluj sprzedaż
                </button>
            ` : ""}
        </div>

        
    </div>
`;

            li.innerHTML = header + body;
            list.appendChild(li);
        });

        M.Collapsible.init(document.querySelectorAll('.collapsible'));
        updateLoadMoreButton(pageData);
    }

    function downloadPdf(id) {
        fetch(`/api/v1/receipt/${id}`)
            .then(resp => resp.blob())
            .then(blob => {
                const url = window.URL.createObjectURL(blob);
                const a = document.createElement("a");
                a.href = url;
                a.download = "potwierdzenie_sprzedazy.pdf";
                a.click();
                window.URL.revokeObjectURL(url);
            });
    }

    function formatDate(dateStr) {
        const d = new Date(dateStr);
        return d.toLocaleString("pl-PL");
    }

    function formatDateOnlyString(dateStr) {
        const d = new Date(dateStr);
        return d.toLocaleDateString("pl-PL");
    }

    function formatDateOnly(date) {
        const d = new Date(date);
        let month = String(d.getMonth() + 1);
        let day = String(d.getDate());
        const year = d.getFullYear();

        if (month.length < 2) month = "0" + month;
        if (day.length < 2) day = "0" + day;

        return [year, month, day].join("-");
    }

    function updateLoadMoreButton(pageData) {
        const loadMoreBtn = document.getElementById("loadMoreBtn");
        if (!loadMoreBtn) return;
        loadMoreBtn.style.display = pageData.last ? "none" : "inline-block";
    }

    function debounce(func, wait) {
        let timeout;
        return function() {
            clearTimeout(timeout);
            timeout = setTimeout(() => func.apply(this, arguments), wait);
        };
    }

    function escapeHtml(value) {
        return String(value ?? "")
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    function downloadServiceInvoice(id) {
        fetch(`/api/v1/receipt/${id}/service-invoice`)
            .then(resp => resp.blob())
            .then(blob => {
                const url = window.URL.createObjectURL(blob);
                const a = document.createElement("a");
                a.href = url;
                a.download = "faktura_serwisowa.pdf";
                a.click();
                window.URL.revokeObjectURL(url);
            });
    }

    document.addEventListener("DOMContentLoaded", () => {
        M.Modal.init(document.querySelectorAll('.modal'));
    });

    let receiptToCancel = null;

    function confirmCancelReceipt(id) {
        receiptToCancel = id;
        const modal = M.Modal.getInstance(
            document.getElementById("cancelModal")
        );
        modal.open();
    }

    document.getElementById("confirmCancelBtn")
        .addEventListener("click", async () => {

            try {
                const resp = await fetch(
                    `/api/v1/receipt/${receiptToCancel}/cancel`,
                    { method: "POST" }
                );

                if (!resp.ok) {
                    // ⬇️ SPRÓBUJ WYCIĄGNĄĆ KOMUNIKAT Z BACKENDU
                    let message = "Błąd anulowania sprzedaży";

                    try {
                        const data = await resp.json();
                        if (data.message) {
                            message = data.message;
                        }
                    } catch {
                        // backend mógł zwrócić plain text albo pustą odpowiedź
                    }

                    throw new Error(message);
                }

                M.toast({
                    html: "Sprzedaż anulowana",
                    classes: "green"
                });

                location.reload();

            } catch (err) {
                M.toast({
                    html: err.message || "Błąd anulowania sprzedaży",
                    classes: "red"
                });
            }
        });
