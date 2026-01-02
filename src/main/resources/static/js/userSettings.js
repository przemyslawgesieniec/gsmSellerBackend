document.addEventListener("DOMContentLoaded", () => {
    fetch("/api/v1/users/me")
        .then(r => r.text())
        .then(console.log);
    initLocationSelect();
    loadLocations();
});

function loadLocations() {
    fetch("/api/v1/locations")
        .then(res => res.json())
        .then(locations => {

            const select = document.getElementById("locationSelect");
            if (!select || !select.tomselect) return;

            const ts = select.tomselect;

            // czyścimy wszystko
            ts.clearOptions();

            // opcja domyślna
            ts.addOption({
                value: "",
                text: "Wybierz lokalizację"
            });

            // backend
            locations.forEach(loc => {
                ts.addOption({
                    value: loc.technicalId,
                    text: `${loc.name}${loc.city ? " – " + loc.city : ""}`
                });
            });

            ts.refreshOptions(false);

            // zmiana lokalizacji
            select.onchange = () => {
                assignLocation(select.value);
            };
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
function initLocationSelect() {
    const select = document.getElementById('locationSelect');
    if (!select) return;

    new TomSelect(select, {
        controlInput: null,
        create: false,
        searchField: [],
        shouldSort: false,
        closeAfterSelect: true,
        placeholder: "Wybierz lokalizację"
    });
}
