package pl.gesieniec.gsmseller.report.sales;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.gesieniec.gsmseller.phone.stock.PhoneStock;
import java.math.RoundingMode;
import pl.gesieniec.gsmseller.phone.stock.StockReportService;
import pl.gesieniec.gsmseller.phone.stock.model.Status;
import pl.gesieniec.gsmseller.receipt.ReceiptRepository;
import pl.gesieniec.gsmseller.repair.Repair;
import pl.gesieniec.gsmseller.repair.RepairRepository;
import pl.gesieniec.gsmseller.repair.model.RepairStatus;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final StockReportService stockReportService;
    private final RepairRepository repairRepository;

    public SalesSummaryDto getSalesSummary(
        LocalDate from,
        LocalDate to
    ) {
        LocalDateTime fromDate = from.atStartOfDay();
        LocalDateTime toDate = to.atTime(LocalTime.MAX);

        List<PhoneStock> sold =
            stockReportService.findSoldBetween(fromDate, toDate);

        BigDecimal turnover = BigDecimal.ZERO;
        BigDecimal cost = BigDecimal.ZERO;

        for (PhoneStock p : sold) {
            turnover = turnover.add(p.getSoldFor());
            cost = cost.add(p.getPurchasePrice());
        }

        BigDecimal profit = turnover.subtract(cost);

        BigDecimal marginPercent = BigDecimal.ZERO;
        if (turnover.compareTo(BigDecimal.ZERO) > 0) {
            marginPercent = profit
                .divide(turnover, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        }

        BigDecimal miscGrossAmount = receiptRepository.sumMiscGrossAmountBetween(fromDate, toDate);
        if (miscGrossAmount == null) {
            miscGrossAmount = BigDecimal.ZERO;
        }

        return new SalesSummaryDto(
            turnover,
            cost,
            profit,
            marginPercent,
            miscGrossAmount
        );
    }

    public SalesDashboardDto getDashboard(
        LocalDate from,
        LocalDate to
    ) {

        // ===== sprzedaż (jak wcześniej) =====
        LocalDateTime fromDate = from.atStartOfDay();
        LocalDateTime toDate = to.atTime(LocalTime.MAX);

        List<PhoneStock> sold =
            stockReportService.findSoldBetween(fromDate, toDate);

        BigDecimal turnover = BigDecimal.ZERO;
        BigDecimal cost = BigDecimal.ZERO;

        for (PhoneStock p : sold) {
            turnover = turnover.add(p.getSoldFor());
            cost = cost.add(p.getPurchasePrice());
        }

        BigDecimal profit = turnover.subtract(cost);

        BigDecimal marginPercent = BigDecimal.ZERO;
        if (turnover.compareTo(BigDecimal.ZERO) > 0) {
            marginPercent = profit
                .divide(turnover, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        }

        BigDecimal miscGrossAmount = receiptRepository.sumMiscGrossAmountBetween(fromDate, toDate);
        if (miscGrossAmount == null) {
            miscGrossAmount = BigDecimal.ZERO;
        }

        // ===== magazyn / aktywność =====
        return new SalesDashboardDto(
            turnover,
            cost,
            profit,
            marginPercent,
            miscGrossAmount,

            stockReportService.getStockCount(),
            stockReportService.getStockValue(),
            stockReportService.getPotentialProfit(),

            stockReportService.getSoldTodayCount(),
            stockReportService.getTodayTurnover(),
            stockReportService.getTodayProfit()
        );

    }

    public List<DailyProfitDto> getProfitPerDay(
        LocalDate from,
        LocalDate to
    ) {
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.atTime(LocalTime.MAX);

        return stockReportService.findSoldBetween(fromDt, toDt)
            .stream()
            .collect(Collectors.groupingBy(
                p -> p.getSoldAt().toLocalDate(),
                Collectors.mapping(
                    p -> p.getSoldFor().subtract(p.getPurchasePrice()),
                    Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                )
            ))
            .entrySet()
            .stream()
            .map(e -> new DailyProfitDto(e.getKey(), e.getValue()))
            .sorted(Comparator.comparing(DailyProfitDto::date))
            .toList();
    }

    public RepairReportDto getRepairSummary(LocalDate from, LocalDate to) {
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.atTime(LocalTime.MAX);

        List<Repair> repairs = repairRepository.findAllByArchivedAndCreateDateTimeBetween(true, fromDt, toDt);

        Map<RepairStatus, Long> statusCounts = repairs.stream()
            .collect(Collectors.groupingBy(Repair::getStatus, Collectors.counting()));

        BigDecimal totalProfit = repairs.stream()
            .filter(r -> r.getRepairPrice() != null)
            .filter(Repair::isForCustomer)
            .map(r -> r.getRepairPrice().subtract(Optional.ofNullable(r.getPurchasePrice()).orElse(BigDecimal.ZERO)))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new RepairReportDto((long) repairs.size(), statusCounts, totalProfit);
    }
}
