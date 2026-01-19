package pl.gesieniec.gsmseller.phone.stock.model;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@AllArgsConstructor
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PhoneStockDto {
    private UUID technicalId;
    private String model;
    private String ram;
    private String memory;
    private String color;
    private String imei;
    private String name;
    private String source;
    private Status status;
    private BigDecimal purchasePrice;
    private BigDecimal sellingPrice;
    private String locationName;
    private PurchaseType purchaseType;
}
