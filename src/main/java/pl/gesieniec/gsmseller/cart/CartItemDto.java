package pl.gesieniec.gsmseller.cart;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pl.gesieniec.gsmseller.common.ItemType;

@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CartItemDto {
    private UUID technicalId;
    private String description;
    private BigDecimal price;
    private ItemType itemType;
}
