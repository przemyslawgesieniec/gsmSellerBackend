package pl.gesieniec.gsmseller.phone.stock.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import pl.gesieniec.gsmseller.common.ItemType;
import pl.gesieniec.gsmseller.event.ItemsSoldEvent;
import pl.gesieniec.gsmseller.event.ReceiptCanceledEvent;

@Service
@Slf4j
public class PhoneStockEventHandler {

    private final PhoneSoldHandler phoneSoldHandler;
    private final PhoneReturnHandler phoneReturnHandler;

    public PhoneStockEventHandler(PhoneSoldHandler phoneSoldHandler,
                                  PhoneReturnHandler phoneReturnHandler) {
        this.phoneSoldHandler = phoneSoldHandler;
        this.phoneReturnHandler = phoneReturnHandler;
    }

    @EventListener
    public void handleItemsSold(ItemsSoldEvent event) {
        log.info("📱 Odbieram ItemsSoldEvent – aktualizuję statusy sprzedanych telefonów");

        event.items().stream()
            .filter(item -> item.getItemType().equals(ItemType.PHONE))
            .forEach(item -> {
                phoneSoldHandler.markPhoneSold(item.getTechnicalId(), item.getNettAmount(), item.getSellingInfo());
                log.info("📱 Telefon {} oznaczono jako sprzedany", item.getTechnicalId());
            });
    }

    @EventListener
    public void handleReceiptCanceled(ReceiptCanceledEvent event) {
        log.info("Obsługa telefonu w kontekscie anulowania paragonu: {}", event);
        phoneReturnHandler.returnPhones(
            event.phoneTechnicalIds()
        );
    }
}
