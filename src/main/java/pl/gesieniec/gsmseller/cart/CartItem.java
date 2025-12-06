package pl.gesieniec.gsmseller.cart;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import pl.gesieniec.gsmseller.phone.stock.PhoneStockDto;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)

public class CartItem {

    public CartItem(String description,
                    BigDecimal price,
                    UUID technicalId,
                    ItemType itemType) {
        this.createDateTime = LocalDateTime.now();
        this.description = description;
        this.price = price;
        this.technicalId = technicalId;
        this.itemType = itemType;

    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createDateTime;

    @Column(length = 500)
    private String description;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    private ItemType itemType;

    private UUID technicalId;

    public static CartItem fromPhone(PhoneStockDto phone) {
        String description = String.format("(%s - RAM: %s GB, ROM: %s GB, KOLOR: %s, IMEI: %s",
            phone.getName(), phone.getRam(), phone.getMemory(), phone.getColor(), phone.getImei());

        return new CartItem(description, phone.getSellingPrice(),
            phone.getTechnicalId(), ItemType.PHONE);
    }

    public void update(String description, BigDecimal price) {
        this.description = description;
        this.price = price;
    }
}
