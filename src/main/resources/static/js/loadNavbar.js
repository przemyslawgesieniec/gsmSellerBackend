document.addEventListener("DOMContentLoaded", async function () {
    try {
        // 1️⃣ załaduj navbar HTML
        const response = await fetch("../navbar.html");
        const html = await response.text();
        document.getElementById("navbar").innerHTML = html;

        // 2️⃣ init Materialize
        M.Sidenav.init(document.querySelectorAll('.sidenav'));
        M.Dropdown.init(document.querySelectorAll('.dropdown-trigger'), {
            hover: true,
            constrainWidth: false,
            coverTrigger: false,
            alignment: 'right'
        });

        // 3️⃣ 🔥 ZAŁADUJ UŻYTKOWNIKA ZAWSZE
        await loadCurrentUser();

    } catch (err) {
        console.error("Błąd ładowania navbaru:", err);
    }
});
