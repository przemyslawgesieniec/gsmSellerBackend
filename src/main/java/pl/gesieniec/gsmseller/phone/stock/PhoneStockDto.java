package pl.gesieniec.gsmseller.phone.stock;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.javamoney.moneta.Money;

@AllArgsConstructor
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PhoneStockDto {
    private String model;
    private String ram;
    private String memory;
    private String color;
    private String imei1;
    private String imei2;
    private String name;
    private String source;
    private Money purchasePrice;
    private Money suggestedSellingPrice;
}
