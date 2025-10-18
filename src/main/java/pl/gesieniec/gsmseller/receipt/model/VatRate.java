package pl.gesieniec.gsmseller.receipt.model;

import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class VatRate {

    private BigDecimal value;
    private String name;

    public static final VatRate VAT_23 = new VatRate(BigDecimal.valueOf(0.23), "23%");
    public static final VatRate VAT_8  = new VatRate(BigDecimal.valueOf(0.08), "8%");
    public static final VatRate VAT_5  = new VatRate(BigDecimal.valueOf(0.05), "5%");
    public static final VatRate VAT_0  = new VatRate(BigDecimal.valueOf(0), "0%");
    public static final VatRate VAT_EXEMPT = new VatRate(BigDecimal.valueOf(0), "ZW");
}
