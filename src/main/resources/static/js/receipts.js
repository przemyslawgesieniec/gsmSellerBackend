    let currentPage = 0;
    const pageSize = 10;

    document.addEventListener("DOMContentLoaded", () => {
        loadReceipts();

        document.getElementById("loadMoreBtn").addEventListener("click", () => {
            currentPage++;
            loadReceipts();
        });
    });

    function loadReceipts() {
        fetch(`/api/v1/receipt/list?page=${currentPage}&size=${pageSize}`)
            .then(resp => resp.json())
            .then(data => renderReceipts(data))
            .catch(err => console.error("Błąd pobierania faktur", err));
    }

    function renderReceipts(pageData) {
        const list = document.getElementById("receiptList");

        pageData.content.forEach(receipt => {

            const li = document.createElement("li");
            const statusBadge = receipt.status === "WYCOFANA"
                ? `<span class="new badge red" data-badge-caption="WYCOFANA"></span>`
                : "";

            const header = `
                <div class="collapsible-header"
                     style="display:flex; justify-content:space-between; align-items:center;">
                    <span>
                        <strong>${receipt.number}</strong>
                        ${statusBadge}<br>
                        <small>${formatDate(receipt.createDate)}</small>
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
                        <div><strong>${item.name}</strong></div>

                        <div class="grey-text">
                            Typ: ${item.itemType} <br>
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
        .addEventListener("click", () => {

            fetch(`/api/v1/receipt/${receiptToCancel}/cancel`, {
                method: "POST"
            })
                .then(resp => {
                    if (!resp.ok) throw new Error();
                    M.toast({
                        text: "Sprzedaż anulowana",
                        classes: "green"
                    });
                    location.reload();
                })
                .catch(() => {
                    M.toast({
                        text: "Błąd anulowania sprzedaży",
                        classes: "red"
                    });
                });
        });


