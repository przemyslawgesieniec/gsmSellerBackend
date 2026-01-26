package pl.gesieniec.gsmseller.report.sales;

import java.math.BigDecimal;

public record SalesDashboardDto(

    // sprzedaż (zakres)
    BigDecimal turnover,
    BigDecimal cost,
    BigDecimal profit,
    BigDecimal marginPercent,

    // magazyn
    long stockCount,
    BigDecimal stockValue,
    BigDecimal potentialProfit,

    // dzisiaj
    long soldTodayCount,
    BigDecimal todayTurnover,
    BigDecimal todayProfit
) {}
