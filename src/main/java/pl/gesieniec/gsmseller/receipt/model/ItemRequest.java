package pl.gesieniec.gsmseller.receipt.model;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import pl.gesieniec.gsmseller.common.ItemType;

@ToString
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)

public class ItemRequest {
    private UUID technicalId;
    private ItemType itemType;
    private Integer warrantyMonths;
    private Boolean used;
    private BigDecimal price;
    private String description;
}