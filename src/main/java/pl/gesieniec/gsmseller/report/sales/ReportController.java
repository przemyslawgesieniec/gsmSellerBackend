package pl.gesieniec.gsmseller.report.sales;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/sales-summary")
    public SalesSummaryDto salesSummary(
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to
    ) {
        LocalDate now = LocalDate.now();

        if (from == null || to == null) {
            from = now.withDayOfMonth(1);
            to = now.withDayOfMonth(now.lengthOfMonth());
        }

        return reportService.getSalesSummary(from, to);
    }

    @GetMapping("/sales-dashboard")
    public SalesDashboardDto dashboard(
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate from,

        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate to
    ) {
        LocalDate now = LocalDate.now();

        if (from == null || to == null) {
            from = now.withDayOfMonth(1);
            to = now.withDayOfMonth(now.lengthOfMonth());
        }

        return reportService.getDashboard(from, to);
    }

    @GetMapping("/profit-per-day")
    public List<DailyProfitDto> profitPerDay(
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate from,

        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate to
    ) {
        return reportService.getProfitPerDay(from, to);
    }


}
