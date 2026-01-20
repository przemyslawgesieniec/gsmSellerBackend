package pl.gesieniec.gsmseller.phone.stock;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pl.gesieniec.gsmseller.location.LocationEntity;

import java.math.BigDecimal;
import java.util.UUID;
import pl.gesieniec.gsmseller.phone.stock.model.PurchaseType;
import pl.gesieniec.gsmseller.phone.stock.model.Status;

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
    private LocalDateTime soldAt;
    private String comment;

    @ManyToOne
    @JoinColumn(name = "location_id")
    private LocationEntity location;

    private BigDecimal soldFor;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Enumerated(EnumType.STRING)
    private PurchaseType purchaseType;

    public PhoneStock(UUID technicalId, String model, String ram, String memory, String color, String imei,
                      String name, String source, BigDecimal purchasePrice, BigDecimal sellingPrice,
                      LocationEntity location, Status status, PurchaseType purchaseType, String comment) {

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
        this.purchaseType = purchaseType;
        this.comment = comment;
    }

    public static PhoneStock create(String model, String ram, String memory, String color, String imei,
                                    String name, String source, BigDecimal purchasePrice, BigDecimal sellingPrice, PurchaseType purchaseType) {

        return new PhoneStock(UUID.randomUUID(), model, ram, memory, color, imei, name,
            source, purchasePrice, sellingPrice, null, Status.WPROWADZONY, purchaseType, null);
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
        if (this.status != Status.DOSTĘPNY) {
            throw new IllegalStateException("Phone cannot be sold in status: " + status);
        }

        this.status = Status.SPRZEDANY;
        this.soldFor = soldPrice;
        this.soldAt = LocalDateTime.now();
    }


    public void returnPhone() {
        this.status = Status.DOSTĘPNY;
        this.soldFor = null;
        this.soldAt = null;
    }

    public void acceptAtLocation(LocationEntity location) {
        this.status = Status.DOSTĘPNY;
        this.location = location;
    }

    public void remove() {
        if (!canBeRemoved()) {
            throw new IllegalStateException(
                "Phone cannot be removed in status: " + this.status
            );
        }

        this.status = Status.USUNIĘTY;
    }

    public boolean canBeRemoved() {
        return this.status == Status.WPROWADZONY
            || this.status == Status.DOSTĘPNY;
    }

    public void removeFromLocation() {
        if (!canBeRemovedFromLocation()) {
            throw new IllegalStateException(
                "Phone cannot be removed in status: " + this.status
            );
        }
        this.location = null;
        this.status = Status.WPROWADZONY;
    }

    private boolean canBeRemovedFromLocation() {
        return this.status == Status.DOSTĘPNY;
    }

    public void handover(String comment, String price) {
        if (this.status != Status.DOSTĘPNY) {
            throw new IllegalStateException(
                "Phone cannot be handed over in status: " + status
            );
        }
        this.status = Status.ODDANY;
        this.comment = comment;
        this.soldFor = new BigDecimal(price);
        this.sellingPrice = new BigDecimal(price);
        this.soldAt = LocalDateTime.now();
    }

}
