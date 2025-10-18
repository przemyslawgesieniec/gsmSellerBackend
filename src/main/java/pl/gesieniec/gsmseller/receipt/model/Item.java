package pl.gesieniec.gsmseller.receipt.model;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public class Item {
    private final String name;
    private final BigDecimal nettAmount;
    private final VatRate vat;
    private final BigDecimal vatAmount;
    private final BigDecimal grossAmount;

    public Item(String name, BigDecimal nettAmount, VatRate vat) {
        this.name = name;
        this.nettAmount = nettAmount;
        this.vat = vat;
        this.vatAmount = nettAmount.multiply(vat.getValue()).setScale(2, BigDecimal.ROUND_HALF_EVEN);
        this.grossAmount = nettAmount.add(vatAmount);
    }
}
