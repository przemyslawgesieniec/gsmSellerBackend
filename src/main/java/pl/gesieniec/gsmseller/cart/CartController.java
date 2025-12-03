package pl.gesieniec.gsmseller.cart;

import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.gesieniec.gsmseller.phone.stock.PhoneStockDto;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@Slf4j
public class CartController {

    private final CartService cartService;

    @PostMapping("/add")
    public ResponseEntity<Cart> addToCart(Principal principal, @RequestParam String technicalId) {

        if (principal == null) {
            log.warn("⚠ Próba dodania telefonu do koszyka, ale Principal jest null!");
            return ResponseEntity.badRequest().build();
        }

        String username = principal.getName();
        log.info("🛒 [{}] Dodaje telefon (technicalId={}) do koszyka", username, technicalId);

        Cart updated = cartService.addPhoneToCart(username, technicalId);

        log.info("🛒 [{}] Koszyk po dodaniu zawiera {} elementów",
            username, updated.getPhoneIds().size());

        return ResponseEntity.ok(updated);
    }

    @GetMapping
    public ResponseEntity<Cart> getCart(Principal principal) {

        if (principal == null) {
            log.warn("⚠ Próba pobrania koszyka, ale Principal jest null!");
            return ResponseEntity.badRequest().build();
        }

        String username = principal.getName();
        log.info("📥 [{}] Pobiera koszyk", username);

        Cart cart = cartService.getCart(username);

        log.info("📥 [{}] Koszyk zawiera {} elementów",
            username, cart.getPhoneIds().size());

        return ResponseEntity.ok(cart);
    }

    @DeleteMapping("/{technicalId}")
    public ResponseEntity<Cart> removeFromCart(Principal principal, @PathVariable String technicalId) {

        if (principal == null) {
            log.warn("⚠ Próba usunięcia telefonu z koszyka, ale Principal jest null!");
            return ResponseEntity.badRequest().build();
        }

        String username = principal.getName();
        log.info("🗑 [{}] Usuwa telefon (technicalId={}) z koszyka", username, technicalId);

        Cart after = cartService.removeFromCart(username, technicalId);

        log.info("🗑 [{}] Koszyk po usunięciu zawiera {} elementów",
            username, after.getPhoneIds().size());

        return ResponseEntity.ok(after);
    }

    @GetMapping("/phones")
    public ResponseEntity<List<PhoneStockDto>> getCartPhones(Principal principal) {
        String username = principal.getName();
        List<PhoneStockDto> phones = cartService.getPhonesInCart(username);
        return ResponseEntity.ok(phones);
    }

}
