package pl.gesieniec.gsmseller.receipt.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import pl.gesieniec.gsmseller.receipt.entity.ReceiptStatus;

@ToString
@AllArgsConstructor
@Getter
public class Receipt {

    private final String number;
    private final UUID technicalId;
    private final ReceiptStatus status;
    private final List<Item> items;
    private final Seller seller;
    private final DateAndPlace dateAndPlace;
    private final String createdBy;
    private final String customerNote;

    public static Receipt of(String number, List<Item> items, Seller seller, DateAndPlace dateAndPlace, String username, String customerNote) {
        UUID technicalId = UUID.randomUUID();
        return new Receipt(number, technicalId, ReceiptStatus.AKTYWNA, items, seller, dateAndPlace, username, customerNote);
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

    public Receipt withVat(VatRate vatRate) {

        List<Item> newItems = this.items.stream()
            .map(item -> item.withVat(vatRate))
            .toList();

        return new Receipt(
            this.number,
            this.technicalId,
            this.status,
            newItems,
            this.seller,
            this.dateAndPlace,
            this.createdBy,
            this.customerNote
        );
    }

    public Receipt canceled() {
        return new Receipt(
            this.number,
            this.technicalId,
            ReceiptStatus.WYCOFANA,
            this.items,
            this.seller,
            this.dateAndPlace,
            this.createdBy,
            this.customerNote
        );
    }

}
