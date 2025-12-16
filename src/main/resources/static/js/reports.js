document.addEventListener("DOMContentLoaded", loadReport);

function loadReport() {
    fetch("/api/reports/sales-summary")
        .then(res => res.json())
        .then(data => {
            document.getElementById("turnover").innerText =
                formatCurrency(data.turnover);

            document.getElementById("cost").innerText =
                formatCurrency(data.cost);

            document.getElementById("profit").innerText =
                formatCurrency(data.profit);

            renderChart(data);
        });
}

function renderChart(data) {
    const ctx = document.getElementById("profitChart");

    new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: ['Koszt', 'Zysk'],
            datasets: [{
                data: [data.cost, data.profit],
                backgroundColor: ['#ff9800', '#4caf50']
            }]
        },
        options: {
            plugins: {
                legend: {
                    position: 'bottom'
                }
            }
        }
    });
}

document.addEventListener("DOMContentLoaded", () => {
    loadReport("THIS_MONTH");

    const select = document.getElementById("dateRange");
    select.addEventListener("change", () => {
        loadReport(select.value);
    });
});

function loadReport(range) {
    const params = buildDateParams(range);
    const url = params
        ? `/api/reports/sales-summary?from=${params.from}&to=${params.to}`
        : `/api/reports/sales-summary`;

    fetch(url)
        .then(res => res.json())
        .then(data => {
            document.getElementById("turnover").innerText =
                formatCurrency(data.turnover);
            document.getElementById("cost").innerText =
                formatCurrency(data.cost);
            document.getElementById("profit").innerText =
                formatCurrency(data.profit);
        });
}

function buildDateParams(range) {
    const today = new Date();

    switch (range) {
        case "THIS_MONTH":
            return {
                from: formatDate(new Date(today.getFullYear(), today.getMonth(), 1)),
                to: formatDate(new Date(today.getFullYear(), today.getMonth() + 1, 0))
            };

        case "LAST_MONTH":
            return {
                from: formatDate(new Date(today.getFullYear(), today.getMonth() - 1, 1)),
                to: formatDate(new Date(today.getFullYear(), today.getMonth(), 0))
            };

        case "THIS_YEAR":
            return {
                from: formatDate(new Date(today.getFullYear(), 0, 1)),
                to: formatDate(new Date(today.getFullYear(), 11, 31))
            };

        case "ALL_TIME":
            return null;
    }
}

function formatDate(date) {
    return date.toISOString().split('T')[0];
}

function formatCurrency(value) {
    return new Intl.NumberFormat('pl-PL', {
        style: 'currency',
        currency: 'PLN'
    }).format(value);
}
