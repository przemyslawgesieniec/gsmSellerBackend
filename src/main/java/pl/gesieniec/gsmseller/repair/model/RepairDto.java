package pl.gesieniec.gsmseller.repair.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepairDto {
    private UUID technicalId;
    private UUID clientTechnicalId;
    private String clientName;
    private String clientSurname;
    private String clientPhoneNumber;

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
    private List<String> photoUrls;

    private RepairStatus status;
    private boolean forCustomer;
    private UUID phoneTechnicalId;
    private LocalDateTime createDateTime;
    private LocalDateTime handoverDate;

    // For compatibility with old fields if necessary
    private BigDecimal purchasePrice;
    private BigDecimal repairPrice;
    private String damageDescription;
    private String repairOrderDescription;
    private String pinPassword;
}
