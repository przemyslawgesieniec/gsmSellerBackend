package pl.gesieniec.gsmseller.receipt.model;

import java.math.BigDecimal;
import java.util.Arrays;
import java.math.RoundingMode;

public enum VatRate {

    VAT_23("23%", new BigDecimal("0.23")),
    VAT_8 ("8%",  new BigDecimal("0.08")),
    VAT_5 ("5%",  new BigDecimal("0.05")),
    VAT_0 ("0%",  BigDecimal.ZERO),
    VAT_EXEMPT("ZW", BigDecimal.ZERO);

    private final String code;
    private final BigDecimal rate;

    VatRate(String code, BigDecimal rate) {
        this.code = code;
        this.rate = rate;
    }

    public String getName() {
        return code;
    }

    public BigDecimal getValue() {
        return rate;
    }

    public static VatRate parse(String value) {
        return Arrays.stream(values())
            .filter(v -> v.code.equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() ->
                new IllegalArgumentException("Nieznana stawka VAT: " + value)
            );
    }

    // -----------------------------
    //  OBLICZENIA
    // -----------------------------

    /** Kwota VAT = netto * stawka */
    public BigDecimal vatAmount(BigDecimal net) {
        return net
            .multiply(rate)
            .setScale(2, RoundingMode.HALF_UP);
    }

    /** Kwota brutto = netto + VAT */
    public BigDecimal grossAmount(BigDecimal net) {
        return net
            .add(vatAmount(net))
            .setScale(2, RoundingMode.HALF_UP);
    }
}
