package pl.gesieniec.gsmseller.receipt.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class Receipt {

    private final String number;
    private final UUID technicalId;
    private final List<Item> items;
    private final Seller seller;
    private final DateAndPlace dateAndPlace;

    public static Receipt of(String number, List<Item> items, Seller seller, DateAndPlace dateAndPlace) {
        UUID technicalId = UUID.randomUUID();
        return new Receipt(number, technicalId, items, seller, dateAndPlace);
    }

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
