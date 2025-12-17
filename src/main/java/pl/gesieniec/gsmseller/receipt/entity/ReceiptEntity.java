package pl.gesieniec.gsmseller.receipt.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Setter
@ToString
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Entity
public class ReceiptEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private UUID technicalId;

    @CreatedDate
    private LocalDateTime createDate;

    @Column(unique = true)
    private String number;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "name", column = @Column(name = "seller_name")),
        @AttributeOverride(name = "street", column = @Column(name = "seller_street")),
        @AttributeOverride(name = "postalCode", column = @Column(name = "seller_postal_code")),
        @AttributeOverride(name = "city", column = @Column(name = "seller_city")),
        @AttributeOverride(name = "nip", column = @Column(name = "seller_nip"))
    })
    private SellerEntity seller;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "place", column = @Column(name = "date_place")),
        @AttributeOverride(name = "generateDate", column = @Column(name = "date_generate_date")),
        @AttributeOverride(name = "sellDate", column = @Column(name = "date_sell_date"))
    })
    private DateAndPlaceEntity dateAndPlace;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "receipt_id")
    private List<ReceiptItemEntity> items;

    @Column(nullable = false)
    private String createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReceiptStatus status = ReceiptStatus.AKTYWNA;


}
