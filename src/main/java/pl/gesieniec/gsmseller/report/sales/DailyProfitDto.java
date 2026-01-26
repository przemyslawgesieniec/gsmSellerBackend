package pl.gesieniec.gsmseller.report.sales;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyProfitDto(
    LocalDate date,
    BigDecimal profit
) {}
