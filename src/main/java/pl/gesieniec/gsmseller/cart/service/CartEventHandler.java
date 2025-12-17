package pl.gesieniec.gsmseller.cart.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import pl.gesieniec.gsmseller.event.ItemsSoldEvent;
import pl.gesieniec.gsmseller.phone.stock.event.PhoneRemovedEvent;

@Service
@Slf4j
public class CartEventHandler {

    private final CartService cartService;

    public CartEventHandler(CartService cartService) {
        this.cartService = cartService;
    }

    @EventListener
    public void onItemsSold(ItemsSoldEvent event) {
        log.info("🧹 Czyszczę koszyk użytkownika {}", event.username());
        cartService.clearCart(event.username());
    }

    @EventListener
    public void onPhoneRemoved(PhoneRemovedEvent event) {
        log.info(
            "🧹 Telefon {} został usunięty – usuwam z koszyków",
            event.phoneTechnicalId()
        );

        cartService.removePhoneFromCart(
            event.phoneTechnicalId()
        );
    }
}

