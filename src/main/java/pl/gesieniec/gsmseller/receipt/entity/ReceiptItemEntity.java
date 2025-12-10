package pl.gesieniec.gsmseller.receipt.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;
import pl.gesieniec.gsmseller.common.ItemType;

@ToString
@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReceiptItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ====== PODSTAWOWE POLA SPRZEDAŻY ======
    private String name;
    private BigDecimal nettAmount;

    private BigDecimal vatRate;

    private BigDecimal vatAmount;
    private BigDecimal grossAmount;

    // ====== DODATKOWE DANE DLA TELEFONÓW ======

    @Column(nullable = true)
    private UUID technicalId;

    private Integer warrantyMonths;

    private Boolean used;

    @Enumerated(EnumType.STRING)
    private ItemType itemType;
}
