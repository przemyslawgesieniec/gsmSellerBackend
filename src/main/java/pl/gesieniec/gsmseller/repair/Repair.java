package pl.gesieniec.gsmseller.repair;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pl.gesieniec.gsmseller.repair.model.RepairStatus;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
@Table(name = "repairs")
public class Repair {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(unique = true, nullable = false)
    private UUID technicalId;

    @Column(nullable = false)
    private String name;

    private String imei;
    private String color;
    private BigDecimal purchasePrice;
    private BigDecimal repairPrice;
    private String damageDescription;
    private String repairOrderDescription;
    private String pinPassword;

    @Column(nullable = false)
    private boolean forCustomer;

    private UUID phoneTechnicalId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RepairStatus status;

    private LocalDateTime createDateTime;

    private Repair(String name, String imei, String color, BigDecimal purchasePrice,
                  BigDecimal repairPrice, String damageDescription, String repairOrderDescription,
                  String pinPassword, boolean isForCustomer, UUID phoneTechnicalId) {
        this.technicalId = UUID.randomUUID();
        this.name = name;
        this.imei = imei;
        this.color = color;
        this.purchasePrice = purchasePrice;
        this.repairPrice = repairPrice;
        this.damageDescription = damageDescription;
        this.repairOrderDescription = repairOrderDescription;
        this.pinPassword = pinPassword;
        this.forCustomer = isForCustomer;
        this.phoneTechnicalId = phoneTechnicalId;
        this.status = RepairStatus.DO_NAPRAWY;
        this.createDateTime = LocalDateTime.now();
    }

    public static Repair create(String name, String imei, String color, BigDecimal purchasePrice,
                               BigDecimal repairPrice, String damageDescription, String repairOrderDescription,
                               String pinPassword, boolean isForCustomer, UUID phoneTechnicalId) {
        return new Repair(name, imei, color, purchasePrice, repairPrice, damageDescription, repairOrderDescription, pinPassword, isForCustomer, phoneTechnicalId);
    }

    public void updateStatus(RepairStatus newStatus) {
        this.status = newStatus;
    }

    public void update(String name, String imei, String color, BigDecimal purchasePrice,
                       BigDecimal repairPrice, String damageDescription, String repairOrderDescription,
                       String pinPassword, Boolean isForCustomer, UUID phoneTechnicalId) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (imei != null) {
            this.imei = imei;
        }
        if (color != null) {
            this.color = color;
        }
        if (purchasePrice != null) {
            this.purchasePrice = purchasePrice;
        }
        if (repairPrice != null) {
            this.repairPrice = repairPrice;
        }
        if (damageDescription != null) {
            this.damageDescription = damageDescription;
        }
        if (repairOrderDescription != null) {
            this.repairOrderDescription = repairOrderDescription;
        }
        if (pinPassword != null) {
            this.pinPassword = pinPassword;
        }
        if (isForCustomer != null) {
            this.forCustomer = isForCustomer;
        }
        if (phoneTechnicalId != null) {
            this.phoneTechnicalId = phoneTechnicalId;
        }
    }
}
