package pl.gesieniec.gsmseller.cart.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import pl.gesieniec.gsmseller.event.ItemsSoldEvent;

@Service
@Slf4j
public class CartCleanerEventHandler {

    private final CartService cartService;

    public CartCleanerEventHandler(CartService cartService) {
        this.cartService = cartService;
    }

    @EventListener
    public void onItemsSold(ItemsSoldEvent event) {
        log.info("🧹 Czyszczę koszyk użytkownika {}", event.username());
        cartService.clearCart(event.username());
    }
}
