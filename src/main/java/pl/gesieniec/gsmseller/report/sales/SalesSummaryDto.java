package pl.gesieniec.gsmseller.report.sales;

import java.math.BigDecimal;

public record SalesSummaryDto(
    BigDecimal turnover,
    BigDecimal cost,
    BigDecimal profit,
    BigDecimal marginPercent,
    BigDecimal miscGrossAmount
) {}
