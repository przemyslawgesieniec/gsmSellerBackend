package pl.gesieniec.gsmseller.receipt.model;

import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ReceiptCreateRequest {
    private String vatRate;
    private List<ItemRequest> items;
}
