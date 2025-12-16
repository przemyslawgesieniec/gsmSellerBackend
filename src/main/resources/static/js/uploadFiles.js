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
        showProgressLoader();

        const totalSteps = selectedFiles.length * 2;
        let currentStep = 0;
        try {
            console.log("Kliknięto Dodaj");

            const formData = new FormData();
            formData.append("name", document.getElementById("manualName").value);
            formData.append("initialPrice", document.getElementById("manualInitialPrice").value);
            formData.append("sellingPrice", document.getElementById("manualSellingPrice").value);
            formData.append("source", document.getElementById("manualSource").value);

            for (const originalFile of selectedFiles) {

                let file = originalFile;

                if (
                    file.type === "image/heic" ||
                    file.type === "image/heif" ||
                    file.name.toLowerCase().endsWith(".heic") ||
                    file.name.toLowerCase().endsWith(".heif")
                ) {

                    updateProgress(
                        Math.round((currentStep / totalSteps) * 100),
                        `Konwersja HEIC: ${file.name}`
                    );

                    console.log("Konwersja HEIC:", file.name);

                    await nextFrame();

                    file = await convertHeicToJpeg(file);
                    currentStep++;
                }

                const percent = Math.min(
                    100,
                    Math.round((currentStep / totalSteps) * 100)
                );

                updateProgress(
                    percent,
                    `Przetwarzanie: ${file.name}`
                );

                await nextFrame(); // też warto przed canvas

                const compressed = await compressImage(file, {
                    maxWidth: 1600,
                    quality: 0.65,
                    binary: true
                });

                formData.append("photos", compressed);
                currentStep++;
            }

            // 🔹 Wysłanie do backendu
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
            M.toast({ html: "Błąd podczas przetwarzania", classes: "red" });

        } finally {
            hideProgressLoader();
        }
    });

    // === WYŚWIETLANIE WYNIKÓW ===
    function renderPhoneResults(phones) {
        const container = document.getElementById("scanResultsContainer");
        container.innerHTML = "";

        const card = document.createElement("div");
        card.className = "card";

        const content = document.createElement("div");
        content.className = "card-content";

        let html = `<h5 class="blue-text text-darken-2">Wynik skanowania</h5>`;

        phones.forEach((p, i) => {

            html += `
        <div class="scan-item z-depth-1" data-phone-index="${i}" style="padding: 16px; border-radius: 8px; margin-top: 20px;">
            <h6>Telefon #${i + 1}</h6>

            <!-- Nazwa, Model, IMEI -->
            <div class="row">
                <div class="input-field col s12 m4">
                    <input type="text" value="${p.name || ''}" data-field="name">
                    <label class="active">Nazwa</label>
                </div>

                <div class="input-field col s12 m4">
                    <input type="text" value="${p.model || ''}" data-field="model">
                    <label class="active">Model</label>
                </div>

                <div class="input-field col s12 m4">
                    <input type="text" value="${p.imei || ''}" data-field="imei">
                    <label class="active">IMEI</label>
                </div>
            </div>

            <!-- RAM, Memory, Color -->
            <div class="row">
                <div class="input-field col s12 m4">
                    <input type="text" value="${p.ram || ''}" data-field="ram">
                    <label class="active">RAM</label>
                </div>

                <div class="input-field col s12 m4">
                    <input type="text" value="${p.memory || ''}" data-field="memory">
                    <label class="active">Pamięć</label>
                </div>

                <div class="input-field col s12 m4">
                    <input type="text" value="${p.color || ''}" data-field="color">
                    <label class="active">Kolor</label>
                </div>
            </div>

            <!-- Source, InitialPrice, SellingPrice -->
            <div class="row">
                <div class="input-field col s12 m4">
                    <input type="text" value="${p.source || ''}" data-field="source">
                    <label class="active">Pochodzenie</label>
                </div>

                <div class="input-field col s12 m4">
                    <input type="number" value="${p.initialPrice || ''}" data-field="initialPrice">
                    <label class="active">Cena zakupu</label>
                </div>

                <div class="input-field col s12 m4">
                    <input type="number" value="${p.sellingPrice || ''}" data-field="sellingPrice">
                    <label class="active">Cena sprzedaży</label>
                </div>
            </div>
        </div>`;
        });

        html += `
        <div class="card-action right-align">
            <button class="btn blue darken-2" id="savePhonesBtn">Dodaj</button>
        </div>
    `;

        content.innerHTML = html;
        card.appendChild(content);
        container.appendChild(card);

        M.updateTextFields();

        document.getElementById("savePhonesBtn")
            .addEventListener("click", sendFinalPhones);

    }
});

function sendFinalPhones() {
    const phoneBlocks = document.querySelectorAll(".scan-item");
    const resultList = [];

    phoneBlocks.forEach(block => {
        const inputs = block.querySelectorAll("input[data-field]");
        const phone = {};
        inputs.forEach(input => {
            const key = input.getAttribute("data-field");
            phone[key] = input.value;
        });
        resultList.push(phone);
    });

    fetch("/api/v1/phones", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(resultList)
    })
        .then(res => {
            if (!res.ok) throw new Error("Błąd");

            // Zapisz flagę do localStorage
            localStorage.setItem("phonesSaved", "true");

            // Przeładuj stronę
            location.reload();
        })
        .catch(err => {
            console.error(err);
            M.toast({ html: "Błąd zapisu", classes: "red" });
        });
}

function compressImage(file, options = {}) {
    const {
        maxWidth = 1600,
        quality = 0.7,
        grayscale = true
    } = options;

    return new Promise((resolve, reject) => {
        const img = new Image();
        const reader = new FileReader();

        reader.onload = e => img.src = e.target.result;
        reader.onerror = reject;

        img.onload = () => {
            let { width, height } = img;

            if (width > maxWidth) {
                height = height * (maxWidth / width);
                width = maxWidth;
            }

            const canvas = document.createElement("canvas");
            canvas.width = width;
            canvas.height = height;

            const ctx = canvas.getContext("2d");
            ctx.drawImage(img, 0, 0, width, height);

            if (grayscale) {
                const imageData = ctx.getImageData(0, 0, width, height);
                const data = imageData.data;

                const threshold = 160;

                for (let i = 0; i < data.length; i += 4) {
                    const gray =
                        data[i] * 0.299 +
                        data[i + 1] * 0.587 +
                        data[i + 2] * 0.114;

                    const bw = gray > threshold ? 255 : 0;

                    data[i] = data[i + 1] = data[i + 2] = bw;
                }

                ctx.putImageData(imageData, 0, 0);

            }

            canvas.toBlob(
                blob => {
                    resolve(new File([blob], file.name, {
                        type: "image/jpeg"
                    }));
                },
                "image/jpeg",
                quality
            );
        };

        reader.readAsDataURL(file);
    });
}

async function convertHeicToJpeg(file) {
    const convertedBlob = await heic2any({
        blob: file,
        toType: "image/jpeg",
        quality: 0.8
    });

    return new File(
        [convertedBlob],
        file.name.replace(/\.heic$/i, ".jpg"),
        { type: "image/jpeg" }
    );
}

function showProgressLoader() {
    document.getElementById("loadingOverlay").classList.remove("hide");
    updateProgress(0, "Przygotowanie…");
}

function hideProgressLoader() {
    document.getElementById("loadingOverlay").classList.add("hide");
}

function updateProgress(percent, text) {
    document.getElementById("progressBar").style.width = percent + "%";
    document.getElementById("progressPercent").innerText = percent + "%";

    if (text) {
        document.getElementById("loaderText").innerText = text;
    }
}
function nextFrame() {
    return new Promise(resolve => requestAnimationFrame(resolve));
}
