package pl.gesieniec.gsmseller.receipt.model;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)

public class ItemRequest {
    private UUID technicalId;
    private String itemType;
    private Integer warrantyMonths;
    private Boolean used;
    private BigDecimal price;
    private String description;
}