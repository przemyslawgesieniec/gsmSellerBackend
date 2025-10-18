package pl.gesieniec.gsmseller.receipt.model;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Receipt {
    private String number;
    private List<Item> items;
    private Seller seller;
    private DateAndPlace dateAndPlace;

    public BigDecimal getNetTotal() {
        return items.stream()
            .map(Item::getNettAmount)
            .reduce(BigDecimal::add)
            .orElse(BigDecimal.ZERO);
    }

    public BigDecimal getGrossTotal() {
        return items.stream()
            .map(Item::getGrossAmount)
            .reduce(BigDecimal::add)
            .orElse(BigDecimal.ZERO);
    }

    public BigDecimal getVatTotal() {
        return items.stream()
            .map(Item::getVatAmount)
            .reduce(BigDecimal::add)
            .orElse(BigDecimal.ZERO);
    }
}
