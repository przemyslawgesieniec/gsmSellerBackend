package pl.gesieniec.gsmseller.report.sales;

import java.math.BigDecimal;

public interface SalesView {
    BigDecimal getSoldFor();
    BigDecimal getPurchasePrice();
}
