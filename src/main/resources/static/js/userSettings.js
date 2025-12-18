document.addEventListener("DOMContentLoaded", () => {

    //sprawdzenie istnienia użytkownika
    fetch("/api/v1/users/me")
        .then(r => r.text())
        .then(console.log);

    loadLocations();

});

function loadLocations() {
    fetch("/api/v1/locations")
        .then(res => res.json())
        .then(locations => {
            const select = document.getElementById("locationSelect");

            locations.forEach(loc => {
                const option = document.createElement("option");
                option.value = loc.technicalId;
                option.textContent = `${loc.name}${loc.city ? " – " + loc.city : ""}`;
                select.appendChild(option);
            });

            M.FormSelect.init(select);

            select.addEventListener("change", () => {
                assignLocation(select.value);
            });
        })
        .catch(() => {
            M.toast({
                html: "Błąd ładowania lokalizacji",
                classes: "red"
            });
        });
}

function assignLocation(technicalId) {

        fetch(`/api/v1/users/location/${technicalId}`, {
            method: "PUT"
        })
        .then(res => {
            if (!res.ok) throw new Error();
            M.toast({
                html: "Lokalizacja została zapisana",
                classes: "green"
            });
        })
        .catch(() => {
            M.toast({
                html: "Nie udało się zapisać lokalizacji",
                classes: "red"
            });
        });
}
