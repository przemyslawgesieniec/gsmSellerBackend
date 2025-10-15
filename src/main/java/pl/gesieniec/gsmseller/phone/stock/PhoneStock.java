package pl.gesieniec.gsmseller.phone.stock;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
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
    private String imei1;
    private String imei2;
    private String name;
    private String source;
    private Money purchasePrice;
    private Money suggestedSellingPrice;

    public PhoneStock(String model, String ram, String memory, String color, String imei1, String imei2) {
        this.model = model;
        this.ram = ram;
        this.memory = memory;
        this.color = color;
        this.imei1 = imei1;
        this.imei2 = imei2;
    }
}
