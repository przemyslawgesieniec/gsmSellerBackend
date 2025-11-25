document.addEventListener("DOMContentLoaded", () => {
    const dropArea = document.getElementById("dropArea");
    const fileInput = document.getElementById("fileInput");
    const addBtn = document.getElementById("addBulkBtn");
    let selectedFiles = [];

    dropArea.addEventListener("click", () => fileInput.click());
    fileInput.addEventListener("change", e => addFiles(e.target.files));

    dropArea.addEventListener("dragover", e => {
        e.preventDefault();
        dropArea.classList.add("hover");
    });
    dropArea.addEventListener("dragleave", () => dropArea.classList.remove("hover"));
    dropArea.addEventListener("drop", e => {
        e.preventDefault();
        dropArea.classList.remove("hover");
        addFiles(e.dataTransfer.files);
    });

    function addFiles(files) {
        for (let f of files) selectedFiles.push(f);
        updateDropArea();
    }

    function updateDropArea() {
        if (selectedFiles.length === 0) {
            dropArea.innerHTML = `<p>Przeciągnij zdjęcia tutaj lub kliknij, aby wybrać z dysku</p>`;
        } else {
            dropArea.innerHTML = `
                <p><strong>${selectedFiles.length}</strong> plików wybranych</p>
                <p>Kliknij, aby dodać więcej</p>
            `;
        }
    }

    // === WYSYŁANIE ===
    addBtn.addEventListener("click", async () => {

        console.log("Kliknięto Dodaj");

        const formData = new FormData();
        formData.append("name", document.getElementById("manualName").value);
        formData.append("price", document.getElementById("manualPrice").value);
        formData.append("source", document.getElementById("manualSource").value);

        selectedFiles.forEach(f => formData.append("photos", f));

        try {
            const response = await fetch("/api/v1/phones", {
                method: "POST",
                body: formData
            });

            console.log("HTTP status:", response.status);

            if (!response.ok) {
                throw new Error("Błąd API");
            }

            const data = await response.json();
            console.log("Response data:", data);

            renderPhoneResults(data);

        } catch (err) {
            console.error("ERROR:", err);
            M.toast({html: "Błąd podczas przetwarzania", classes: "red"});
        }
    });

    // === WYŚWIETLANIE WYNIKÓW ===
    function renderPhoneResults(phones) {

        console.log("renderPhoneResults wywołane:", phones);

        const container = document.getElementById("scanResultsContainer");
        container.innerHTML = "";

        const card = document.createElement("div");
        card.className = "card";

        const content = document.createElement("div");
        content.className = "card-content";

        let html = `<h5 class="blue-text text-darken-2">Wynik skanowania</h5>`;

        phones.forEach((p, i) => {

            html += `
                <div class="scan-item z-depth-1" style="padding: 16px; border-radius: 8px; margin-top: 20px;">
                    <h6>Telefon #${i + 1}</h6>

                    <div class="row">
                        <div class="input-field col s12 m4">
                            <input type="text" value="${p.name || ''}">
                            <label class="active">Nazwa</label>
                        </div>
                        <div class="input-field col s12 m4">
                            <input type="text" value="${p.model || ''}">
                            <label class="active">Model</label>
                        </div>
                        <div class="input-field col s12 m4">
                            <input type="text" value="${p.imei || ''}">
                            <label class="active">IMEI</label>
                        </div>
                    </div>

                    <div class="row">
                        <div class="input-field col s12 m4">
                            <input type="text" value="${p.ram || ''}">
                            <label class="active">RAM</label>
                        </div>
                        <div class="input-field col s12 m4">
                            <input type="text" value="${p.memory || ''}">
                            <label class="active">Pamięć</label>
                        </div>
                        <div class="input-field col s12 m4">
                            <input type="text" value="${p.color || ''}">
                            <label class="active">Kolor</label>
                        </div>
                    </div>

                    <div class="row">
                        <div class="input-field col s12 m6">
                            <input type="text" value="${p.source || ''}">
                            <label class="active">Pochodzenie</label>
                        </div>
                        <div class="input-field col s12 m6">
                            <input type="text" value="${p.initPrice || ''}">
                            <label class="active">Cena zakupu</label>
                        </div>
                    </div>
                </div>`;
        });

        html += `
            <div class="card-action right-align">
                <button class="btn blue darken-2">Dodaj</button>
            </div>
        `;

        content.innerHTML = html;
        card.appendChild(content);
        container.appendChild(card);

        M.updateTextFields(); // ważne dla Materialize
    }
});
