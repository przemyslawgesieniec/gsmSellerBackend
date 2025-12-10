document.addEventListener("DOMContentLoaded", function () {

    fetch("../navbar.html")
        .then(response => response.text())
        .then(html => {
            document.getElementById("navbar").innerHTML = html;

            // Materialize init
            M.Sidenav.init(document.querySelectorAll('.sidenav'));

            // INIT DROPDOWN (po najechaniu)
            M.Dropdown.init(document.querySelectorAll('.dropdown-trigger'), {
                hover: true,
                constrainWidth: false,
                coverTrigger: false,
                alignment: 'right'
            });
        })
        .catch(err => console.error("Błąd ładowania navbaru:", err));
});
