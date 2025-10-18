package pl.gesieniec.gsmseller.receipt.model;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public class Item {
    private final String name;
    private final BigDecimal nettAmount;
    private final VatRate vatRate;
    private final BigDecimal vatAmount;
    private final BigDecimal grossAmount;

    public static Item of(String name, BigDecimal nettAmount, VatRate vatRate) {
        BigDecimal vatAmount = nettAmount.multiply(vatRate.getValue())
            .setScale(2, BigDecimal.ROUND_HALF_EVEN);

        BigDecimal grossAmount = nettAmount.add(vatAmount);
        return new Item(name, nettAmount, vatRate, vatAmount, grossAmount);
    }
}
