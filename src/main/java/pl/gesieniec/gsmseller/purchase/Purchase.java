package pl.gesieniec.gsmseller.purchase;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "purchases")
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID technicalId;

    @Column(nullable = false)
    private String phoneModel;

    @Column(nullable = false)
    private String phoneNumber;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PurchaseStatus status;

    @Column(columnDefinition = "TEXT")
    private String closureReason;

    private Boolean contactedCustomer;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_id")
    private List<PurchasePhoto> photos = new ArrayList<>();

    public Purchase(String phoneModel, String phoneNumber, String description, List<PurchasePhoto> photos) {
        this.technicalId = UUID.randomUUID();
        this.phoneModel = phoneModel;
        this.phoneNumber = phoneNumber;
        this.description = description;
        this.status = PurchaseStatus.OPEN;
        this.createdAt = LocalDateTime.now();
        if (photos != null) {
            this.photos.addAll(photos);
        }
    }

    public void close(String reason, boolean contactedCustomer) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Closure reason is required");
        }
        this.status = PurchaseStatus.CLOSED;
        this.closureReason = reason;
        this.contactedCustomer = contactedCustomer;
    }
}
