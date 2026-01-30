package pl.gesieniec.gsmseller.repair.model;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Data;

@Data
public class RestoreToShopRequest {
    private BigDecimal repairPrice;
    private BigDecimal sellingPrice;
    private UUID locationTechnicalId;
}
