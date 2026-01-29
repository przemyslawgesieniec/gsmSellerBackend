package pl.gesieniec.gsmseller.phone.stock.handler;

import java.math.BigDecimal;
import java.util.UUID;

public interface PhoneSoldHandler {
    void markPhoneSold(UUID technicalId, BigDecimal soldPrice, String sellingInfo);
}
