package pl.gesieniec.gsmseller.phone.stock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import pl.gesieniec.gsmseller.common.ItemType;
import pl.gesieniec.gsmseller.event.ItemsSoldEvent;

@Service
@Slf4j
public class PhoneStockEventHandler {

    private final PhoneSoldHandler phoneSoldHandler;

    public PhoneStockEventHandler(PhoneSoldHandler phoneSoldHandler) {
        this.phoneSoldHandler = phoneSoldHandler;
    }

    @EventListener
    public void handleItemsSold(ItemsSoldEvent event) {
        log.info("📱 Odbieram ItemsSoldEvent – aktualizuję statusy sprzedanych telefonów");

        event.items().stream()
            .filter(item -> item.getItemType().equals(ItemType.PHONE))
            .forEach(item -> {
                phoneSoldHandler.markPhoneSold(item.getTechnicalId(), item.getNettAmount());
                log.info("📱 Telefon {} oznaczono jako sprzedany", item.getTechnicalId());
            });
    }
}
