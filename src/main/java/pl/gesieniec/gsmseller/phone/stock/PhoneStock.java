package pl.gesieniec.gsmseller.phone.stock;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.javamoney.moneta.Money;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class PhoneStock {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String model;
    private String ram;
    private String memory;
    private String color;
    private String imei;
    private String name;
    private String source;
    private BigDecimal purchasePrice;
    private BigDecimal suggestedSellingPrice;

    public PhoneStock(String model, String ram, String memory, String color, String imei, String name,
                      String source, BigDecimal purchasePrice, BigDecimal suggestedSellingPrice) {
        this.model = model;
        this.ram = ram;
        this.memory = memory;
        this.color = color;
        this.imei = imei;
        this.name = name;
        this.source = source;
        this.purchasePrice = purchasePrice;
        this.suggestedSellingPrice = suggestedSellingPrice;
    }
}
