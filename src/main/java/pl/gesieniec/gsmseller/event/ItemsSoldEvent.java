package pl.gesieniec.gsmseller.event;

import java.util.List;
import pl.gesieniec.gsmseller.receipt.model.Item;

public record ItemsSoldEvent(
    String username,
    String receiptNumber,
    List<Item> items
) {}
