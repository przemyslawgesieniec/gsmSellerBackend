package pl.gesieniec.gsmseller.cart;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    public ResponseEntity<Cart> addToCart(Principal principal, @RequestParam UUID technicalId) {

        if (principal == null) {
            log.warn("⚠ Próba dodania telefonu do koszyka, ale Principal jest null!");
            return ResponseEntity.badRequest().build();
        }

        String username = principal.getName();
        log.info("✅ [{}] Dodaje telefon (technicalId={}) do koszyka", username, technicalId);

        Cart updated = cartService.addPhoneToCart(username, technicalId);

        log.info("✅ [{}] Koszyk po dodaniu zawiera {} elementów",
            username, updated.getItems().size());

        return ResponseEntity.ok(updated);
    }

    @PostMapping("/add/misc")
    public ResponseEntity<Cart> addMiscToCart(Principal principal, @RequestBody CartItemDto request) {

        if (principal == null) {
            log.warn("⚠ Próba dodania przedmiotu (misc) do koszyka, ale Principal jest null!");
            return ResponseEntity.badRequest().build();
        }

        if (request.getDescription() == null || request.getDescription().isBlank()) {
            log.warn("⚠ Próba dodania MISC bez opisu!");
            return ResponseEntity.badRequest().body(null);
        }

        if (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("⚠ Próba dodania MISC z niepoprawną ceną!");
            return ResponseEntity.badRequest().body(null);
        }

        String username = principal.getName();

        log.info("🛒 [{}] Dodaje przedmiot MISC do koszyka: '{}', {} zł",
            username, request.getDescription(), request.getPrice());

        Cart updated = cartService.addMiscItem(
            username,
            request.getDescription(),
            request.getPrice(),
            request.getTechnicalId()
        );

        log.info("🛒 [{}] Koszyk po dodaniu MISC zawiera {} przedmiotów",
            username,
            updated.getItems().size());

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
            username, cart.getItems().size());

        return ResponseEntity.ok(cart);
    }

    @DeleteMapping("/{technicalId}")
    public ResponseEntity<Cart> removeFromCart(Principal principal, @PathVariable UUID technicalId) {

        if (principal == null) {
            log.warn("⚠ Próba usunięcia telefonu z koszyka, ale Principal jest null!");
            return ResponseEntity.badRequest().build();
        }

        String username = principal.getName();
        log.info("🗑 [{}] Usuwa telefon (technicalId={}) z koszyka", username, technicalId);

        Cart after = cartService.removeFromCart(username, technicalId);

        log.info("🗑 [{}] Koszyk po usunięciu zawiera {} elementów",
            username, after.getItems().size());

        return ResponseEntity.ok(after);
    }

    @GetMapping("/full")
    public ResponseEntity<FullCartViewDto> getCartPhones(Principal principal) {

        if (principal == null) {
            log.warn("⚠ Próba pobrania całego koszyka, ale Principal jest null!");
            return ResponseEntity.badRequest().build();
        }

        String username = principal.getName();
        log.info("🛒  [{}] Pobiera cały koszyk)", username);

        List<PhoneStockDto> phones = cartService.getPhonesInCart(username);
        List<CartItemDto> misc = cartService.getMiscInCart(username);
        FullCartViewDto fullCartViewDto = new FullCartViewDto(misc, phones);

        log.info("🛒  [{}] Cały koszyk zawiera: {})", username, fullCartViewDto);

        return ResponseEntity.ok(fullCartViewDto);
    }
}
