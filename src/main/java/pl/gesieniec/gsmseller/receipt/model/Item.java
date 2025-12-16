package pl.gesieniec.gsmseller.receipt.model;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import pl.gesieniec.gsmseller.common.ItemType;

@ToString
@Getter
@AllArgsConstructor
public class Item {

    // ====== PODSTAWOWE POLA SPRZEDAŻY ======
    private final String name;
    private final BigDecimal nettAmount;
    private final VatRate vatRate;
    private final BigDecimal vatAmount;
    private final BigDecimal grossAmount;

    // ====== DODATKOWE DANE DLA TELEFONÓW ======
    private final UUID technicalId;
    private final Integer warrantyMonths;
    private final Boolean used;
    private final ItemType itemType;


    /**
     * Konstruktor fabryczny dla zwykłych pozycji (MISC)
     */
    public static Item of(String name, BigDecimal nettAmount, VatRate vatRate) {
        BigDecimal vatAmount = nettAmount
            .multiply(vatRate.getValue())
            .setScale(2, BigDecimal.ROUND_HALF_EVEN);

        BigDecimal grossAmount = nettAmount.add(vatAmount);

        return new Item(
            name,
            nettAmount,
            vatRate,
            vatAmount,
            grossAmount,
            null,
            null,
            null,
            ItemType.MISC
        );
    }


    /**
     * Konstruktor fabryczny dla pozycji typu PHONE
     */
    public static Item phone(
        String name,
        BigDecimal nettAmount,
        VatRate vatRate,
        UUID technicalId,
        Integer warrantyMonths,
        Boolean used
    ) {
        BigDecimal vatAmount = nettAmount
            .multiply(vatRate.getValue())
            .setScale(2, BigDecimal.ROUND_HALF_EVEN);

        BigDecimal grossAmount = nettAmount.add(vatAmount);

        return new Item(
            name,
            nettAmount,
            vatRate,
            vatAmount,
            grossAmount,
            technicalId,
            warrantyMonths,
            used,
            ItemType.PHONE
        );
    }

    public String getWarrantyMonthsFormatted(){
        if(warrantyMonths == null || warrantyMonths == 0) {
            return "";
        }
        return "Gwarancja " + warrantyMonths + " mies.";
    }

    public Item withVat(VatRate newVatRate) {

        BigDecimal vatAmount = nettAmount
            .multiply(newVatRate.getValue())
            .setScale(2, BigDecimal.ROUND_HALF_EVEN);

        BigDecimal grossAmount = nettAmount.add(vatAmount);

        return new Item(
            this.name,
            this.nettAmount,
            newVatRate,
            vatAmount,
            grossAmount,
            this.technicalId,
            this.warrantyMonths,
            this.used,
            this.itemType
        );
    }

}

