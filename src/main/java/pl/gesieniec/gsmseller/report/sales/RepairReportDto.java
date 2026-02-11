package pl.gesieniec.gsmseller.report.sales;

import java.math.BigDecimal;
import java.util.Map;
import pl.gesieniec.gsmseller.repair.model.RepairStatus;

public record RepairReportDto(
    long totalRepairs,
    Map<RepairStatus, Long> statusCounts,
    BigDecimal totalProfit
) {}
