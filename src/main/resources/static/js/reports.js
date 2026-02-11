document.addEventListener("DOMContentLoaded", () => {
    M.Sidenav.init(document.querySelectorAll('.sidenav'));

    initDatePicker();

    const { from, to } = getCurrentMonthRange();
    loadSalesSummary(from, to);
    loadProfitPerDay(from, to);
    loadRepairSummary(from, to);
    loadDashboard();
});

/* =========================
   DATE PICKER
========================= */

function initDatePicker() {
    flatpickr("#dateRangePicker", {
        mode: "range",
        dateFormat: "Y-m-d",
        locale: flatpickr.l10ns.pl,
        monthSelectorType: "dropdown",
        defaultDate: [
            getCurrentMonthRange().from,
            getCurrentMonthRange().to
        ],
        onClose(selectedDates) {
            if (selectedDates.length === 2) {
                const from = formatDate(selectedDates[0]);
                const to = formatDate(selectedDates[1]);

                loadSalesSummary(from, to);
                loadProfitPerDay(from, to);
            }
        }
    });

    flatpickr("#repairDateRangePicker", {
        mode: "range",
        dateFormat: "Y-m-d",
        locale: flatpickr.l10ns.pl,
        monthSelectorType: "dropdown",
        defaultDate: [
            getCurrentMonthRange().from,
            getCurrentMonthRange().to
        ],
        onClose(selectedDates) {
            if (selectedDates.length === 2) {
                const from = formatDate(selectedDates[0]);
                const to = formatDate(selectedDates[1]);

                loadRepairSummary(from, to);
            }
        }
    });

}

/* =========================
   SALES SUMMARY
========================= */

function loadSalesSummary(from, to) {
    fetch(`/api/reports/sales-summary?from=${from}&to=${to}`)
        .then(handleJsonResponse)
        .then(data => {
            setText("turnover", formatCurrency(data.turnover));
            setText("cost", formatCurrency(data.cost));
            setText("profit", formatCurrency(data.profit));
            setText("miscGrossAmount", formatCurrency(data.miscGrossAmount));
        })
        .catch(err => console.error("Sales summary error", err));
}

/* =========================
   REPAIR SUMMARY
========================= */

function loadRepairSummary(from, to) {
    fetch(`/api/reports/repair-summary?from=${from}&to=${to}`)
        .then(handleJsonResponse)
        .then(data => {
            setText("totalRepairs", data.totalRepairs);
            setText("repairProfit", formatCurrency(data.totalProfit));

            // Status counts
            setText("repairedCount", data.statusCounts["NAPRAWIONY"] || 0);
            setText("cancelledCount", data.statusCounts["ANULOWANY"] || 0);
            setText("notRepairedCount", data.statusCounts["NIE_DO_NAPRAWY"] || 0);
            setText("inRepairCount", data.statusCounts["W_NAPRAWIE"] || 0);
        })
        .catch(err => console.error("Repair summary error", err));
}

/* =========================
   DASHBOARD
========================= */

function loadDashboard() {
    fetch("/api/reports/sales-dashboard")
        .then(handleJsonResponse)
        .then(d => {
            setText("stockCount", d.stockCount);
            setText("stockValue", formatCurrency(d.stockValue));
            setText("potentialProfit", formatCurrency(d.potentialProfit));

            setText("soldTodayCount", d.soldTodayCount);
            setText("todayTurnover", formatCurrency(d.todayTurnover));
            setText("todayProfit", formatCurrency(d.todayProfit));
            setText("miscGrossAmount", formatCurrency(d.miscGrossAmount));
        })
        .catch(err => console.error("Dashboard error", err));
}

/* =========================
   PROFIT PER DAY CHART
========================= */

let profitChart = null;

function loadProfitPerDay(from, to) {
    fetch(`/api/reports/profit-per-day?from=${from}&to=${to}`)
        .then(handleJsonResponse)
        .then(data => {
            if (!Array.isArray(data) || data.length === 0) {
                destroyChart();
                return;
            }
            renderProfitChart(data);
        })
        .catch(err => {
            console.error("Profit per day error", err);
            destroyChart();
        });
}

function renderProfitChart(data) {
    const ctx = document.getElementById("profitPerDayChart");
    if (!ctx) return;

    destroyChart();

    profitChart = new Chart(ctx, {
        type: "bar",
        data: {
            labels: data.map(d => d.date),
            datasets: [{
                label: "Zysk dzienny",
                data: data.map(d => d.profit),
                borderRadius: 4
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: true, // 🔒 BLOKUJE WZROST
            plugins: {
                legend: { display: false },
                tooltip: {
                    callbacks: {
                        label: ctx => ` ${formatCurrency(ctx.raw)}`
                    }
                }
            },
            scales: {
                x: {
                    ticks: {
                        autoSkip: true,        // ✅ POZWÓL SKIPOWAĆ
                        maxTicksLimit: 10      // 🔥 MAKS 10 ETYKIET
                    }
                },
                y: {
                    beginAtZero: true,
                    ticks: {
                        callback: v => formatCurrency(v)
                    }
                }
            }
        }
    });
}

function destroyChart() {
    if (profitChart) {
        profitChart.destroy();
        profitChart = null;
    }
}

/* =========================
   HELPERS
========================= */

function handleJsonResponse(res) {
    if (!res.ok) {
        throw new Error(`HTTP ${res.status}`);
    }
    return res.json();
}

function setText(id, value) {
    const el = document.getElementById(id);
    if (el) el.innerText = value ?? "—";
}

function formatCurrency(value) {
    if (value == null) return "—";
    return new Intl.NumberFormat("pl-PL", {
        style: "currency",
        currency: "PLN"
    }).format(value);
}

function formatDate(date) {
    return date.toISOString().split("T")[0];
}

function getCurrentMonthRange() {
    const now = new Date();
    return {
        from: formatDate(new Date(now.getFullYear(), now.getMonth(), 1)),
        to: formatDate(new Date(now.getFullYear(), now.getMonth() + 1, 0))
    };
}
