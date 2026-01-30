package pl.gesieniec.gsmseller.repair.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private String name;
    private String imei;
    private String color;
    private BigDecimal purchasePrice;
    private BigDecimal repairPrice;
    private String damageDescription;
    private String repairOrderDescription;
    private String pinPassword;
    private RepairStatus status;
    private boolean forCustomer;
    private UUID phoneTechnicalId;
    private LocalDateTime createDateTime;
}
