package pl.gesieniec.gsmseller.phone.stock;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pl.gesieniec.gsmseller.location.LocationEntity;

import java.math.BigDecimal;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class PhoneStock {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private UUID technicalId;
    private String model;
    private String ram;
    private String memory;
    private String color;
    private String imei;
    private String name;
    private String source;
    private BigDecimal purchasePrice;
    private BigDecimal sellingPrice;

    @ManyToOne
    @JoinColumn(name = "location_id")
    private LocationEntity location;

    private BigDecimal soldFor;

    @Enumerated(EnumType.STRING)
    private Status status;

    public PhoneStock(UUID technicalId, String model, String ram, String memory, String color, String imei,
                      String name, String source, BigDecimal purchasePrice, BigDecimal sellingPrice,
                      LocationEntity location, Status status) {

        this.technicalId = technicalId;
        this.model = model;
        this.ram = ram;
        this.memory = memory;
        this.color = color;
        this.imei = imei;
        this.name = name;
        this.source = source;
        this.purchasePrice = purchasePrice;
        this.sellingPrice = sellingPrice;
        this.location = location;
        this.status = status;
    }

    public static PhoneStock create(String model, String ram, String memory, String color, String imei,
                                    String name, String source, BigDecimal purchasePrice, BigDecimal sellingPrice) {

        return new PhoneStock(UUID.randomUUID(), model, ram, memory, color, imei, name,
            source, purchasePrice, sellingPrice, null, Status.WPROWADZONY);
    }

    public void update(String model, String ram, String memory, String color,
                       String imei, String name, String source, BigDecimal sellingPrice) {

        if (model != null) this.model = model;
        if (ram != null) this.ram = ram;
        if (memory != null) this.memory = memory;
        if (color != null) this.color = color;
        if (imei != null) this.imei = imei;
        if (name != null) this.name = name;
        if (source != null) this.source = source;
        if (sellingPrice != null) this.sellingPrice = sellingPrice;
    }

    public void sell(BigDecimal soldPrice) {
        this.status = Status.SPRZEDANY;
        this.soldFor = soldPrice;
    }

    public void acceptAtLocation(LocationEntity location) {
        this.status = Status.DOSTĘPNY;
        this.location = location;
    }
}
