package pl.gesieniec.gsmseller.cart;

import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import pl.gesieniec.gsmseller.phone.stock.model.PhoneStockDto;

@ToString
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FullCartViewDto {
    private List<CartItemDto> miscItems;
    private List<PhoneStockDto> phones;
}
