package pl.gesieniec.gsmseller.phone.stock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.gesieniec.gsmseller.phone.stock.model.Status;

@Service
@RequiredArgsConstructor
public class StockReportService {

    private final PhoneStockRepository phoneStockRepository;

    public long getStockCount() {
        return phoneStockRepository.countByStatusIn(
            List.of(Status.DOSTĘPNY, Status.WPROWADZONY)
        );
    }

    public BigDecimal getStockValue() {
        return phoneStockRepository.sumPurchasePriceByStatusIn(
            List.of(Status.DOSTĘPNY, Status.WPROWADZONY)
        );
    }

    public BigDecimal getPotentialProfit() {
        return phoneStockRepository.sumPotentialProfitByStatusIn(
            List.of(Status.DOSTĘPNY, Status.WPROWADZONY)
        );
    }

    public long getSoldTodayCount() {
        LocalDateTime from = LocalDate.now().atStartOfDay();
        LocalDateTime to = LocalDate.now().atTime(LocalTime.MAX);

        return phoneStockRepository.countByStatusInAndSoldAtBetween(
            List.of(Status.SPRZEDANY, Status.ODDANY),
            from,
            to
        );
    }

    public List<PhoneStock> findSoldBetween(
        LocalDateTime from,
        LocalDateTime to
    ) {
        return phoneStockRepository.findAll(
            PhoneStockSpecifications.soldBetween(from, to)
        );
    }

    public BigDecimal getTodayTurnover() {
        LocalDateTime from = LocalDate.now().atStartOfDay();
        LocalDateTime to = LocalDate.now().atTime(LocalTime.MAX);

        return phoneStockRepository.sumSoldForBetween(
            List.of(Status.SPRZEDANY, Status.ODDANY),
            from,
            to
        );
    }

    public BigDecimal getTodayProfit() {
        LocalDateTime from = LocalDate.now().atStartOfDay();
        LocalDateTime to = LocalDate.now().atTime(LocalTime.MAX);

        return phoneStockRepository.sumProfitBetween(
            List.of(Status.SPRZEDANY, Status.ODDANY),
            from,
            to
        );
    }

}
