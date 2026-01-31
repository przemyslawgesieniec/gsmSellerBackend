package pl.gesieniec.gsmseller.repair;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pl.gesieniec.gsmseller.repair.client.RepairClient;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private RepairClient client;

    private String manufacturer;
    private String model;
    private String imei;

    private String deviceCondition;
    private String problemDescription;
    private String remarks;

    private boolean moistureTraces;
    private boolean warrantyRepair;
    private boolean turnsOn;

    private String lockCode;

    private LocalDateTime receiptDate;
    private LocalDateTime estimatedRepairDate;
    private BigDecimal estimatedCost;

    @ElementCollection
    @CollectionTable(name = "repair_photos", joinColumns = @JoinColumn(name = "repair_id"))
    @Column(name = "photo_url")
    private List<String> photoUrls = new ArrayList<>();

    @Column(nullable = false)
    private boolean forCustomer;

    private UUID phoneTechnicalId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RepairStatus status;

    private LocalDateTime createDateTime;

    private BigDecimal purchasePrice;
    private BigDecimal repairPrice;

    private Repair(RepairClient client, String manufacturer, String model, String imei,
                  String deviceCondition, String problemDescription, String remarks,
                  boolean moistureTraces, boolean warrantyRepair, boolean turnsOn,
                  String lockCode, LocalDateTime receiptDate, LocalDateTime estimatedRepairDate,
                  BigDecimal estimatedCost, List<String> photoUrls, boolean isForCustomer, UUID phoneTechnicalId,
                  BigDecimal purchasePrice, BigDecimal repairPrice) {
        this.technicalId = UUID.randomUUID();
        this.client = client;
        this.manufacturer = manufacturer;
        this.model = model;
        this.imei = imei;
        this.deviceCondition = deviceCondition;
        this.problemDescription = problemDescription;
        this.remarks = remarks;
        this.moistureTraces = moistureTraces;
        this.warrantyRepair = warrantyRepair;
        this.turnsOn = turnsOn;
        this.lockCode = lockCode;
        this.receiptDate = receiptDate != null ? receiptDate : LocalDateTime.now();
        this.estimatedRepairDate = estimatedRepairDate != null ? estimatedRepairDate : LocalDateTime.now().plusDays(7);
        this.estimatedCost = estimatedCost;
        if (photoUrls != null) {
            this.photoUrls = new ArrayList<>(photoUrls);
        }
        this.forCustomer = isForCustomer;
        this.phoneTechnicalId = phoneTechnicalId;
        this.purchasePrice = purchasePrice;
        this.repairPrice = repairPrice;
        this.status = RepairStatus.DO_NAPRAWY;
        this.createDateTime = LocalDateTime.now();
    }

    public static Repair create(RepairClient client, String manufacturer, String model, String imei,
                               String deviceCondition, String problemDescription, String remarks,
                               boolean moistureTraces, boolean warrantyRepair, boolean turnsOn,
                               String lockCode, LocalDateTime receiptDate, LocalDateTime estimatedRepairDate,
                               BigDecimal estimatedCost, List<String> photoUrls, UUID phoneTechnicalId,
                               BigDecimal purchasePrice, BigDecimal repairPrice) {
        return new Repair(client, manufacturer, model, imei, deviceCondition, problemDescription, remarks,
                moistureTraces, warrantyRepair, turnsOn, lockCode, receiptDate, estimatedRepairDate,
                estimatedCost, photoUrls, true, phoneTechnicalId, purchasePrice, repairPrice);
    }

    public static Repair createInHouseRepair(String model, String imei,
                                            BigDecimal purchasePrice, BigDecimal repairPrice,
                                            String problemDescription, String deviceCondition,
                                            String lockCode, UUID phoneTechnicalId) {
        return new Repair(null, null, model, imei, deviceCondition, problemDescription, null,
                false, false, false, lockCode, null, null, null, null, false,
                phoneTechnicalId, purchasePrice, repairPrice);
    }

    public void updateStatus(RepairStatus newStatus) {
        this.status = newStatus;
    }

    public void update(RepairClient client, String manufacturer, String model, String imei,
                       String deviceCondition, String problemDescription, String remarks,
                       Boolean moistureTraces, Boolean warrantyRepair, Boolean turnsOn,
                       String lockCode, LocalDateTime receiptDate, LocalDateTime estimatedRepairDate,
                       BigDecimal estimatedCost, List<String> photoUrls, UUID phoneTechnicalId,
                       BigDecimal purchasePrice, BigDecimal repairPrice) {
        this.client = client;
        if (manufacturer != null) this.manufacturer = manufacturer;
        if (model != null) this.model = model;
        if (imei != null) this.imei = imei;
        if (deviceCondition != null) this.deviceCondition = deviceCondition;
        if (problemDescription != null) this.problemDescription = problemDescription;
        if (remarks != null) this.remarks = remarks;
        if (moistureTraces != null) this.moistureTraces = moistureTraces;
        if (warrantyRepair != null) this.warrantyRepair = warrantyRepair;
        if (turnsOn != null) this.turnsOn = turnsOn;
        if (lockCode != null) this.lockCode = lockCode;
        if (receiptDate != null) this.receiptDate = receiptDate;
        if (estimatedRepairDate != null) this.estimatedRepairDate = estimatedRepairDate;
        if (estimatedCost != null) this.estimatedCost = estimatedCost;
        if (photoUrls != null) {
            this.photoUrls.clear();
            this.photoUrls.addAll(photoUrls);
        }
        if (phoneTechnicalId != null) this.phoneTechnicalId = phoneTechnicalId;
        if (purchasePrice != null) this.purchasePrice = purchasePrice;
        if (repairPrice != null) this.repairPrice = repairPrice;
    }

    public String getDamageDescription() {
        return problemDescription;
    }

    public String getRepairOrderDescription() {
        return remarks;
    }

    public String getPinPassword() {
        return lockCode;
    }
}
