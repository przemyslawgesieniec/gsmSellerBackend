package pl.gesieniec.gsmseller.report.sales;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.gesieniec.gsmseller.phone.stock.PhoneStock;
import pl.gesieniec.gsmseller.phone.stock.PhoneStockService;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final PhoneStockService phoneStockService;

    public SalesSummaryDto getSalesSummary(
        LocalDate from,
        LocalDate to
    ) {
        LocalDateTime fromDate = from.atStartOfDay();
        LocalDateTime toDate = to.atTime(LocalTime.MAX);

        List<PhoneStock> sold =
            phoneStockService.findSoldBetween(fromDate, toDate);

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

        return new SalesSummaryDto(
            turnover,
            cost,
            profit,
            marginPercent
        );
    }
}
