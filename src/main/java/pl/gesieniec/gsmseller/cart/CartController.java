package pl.gesieniec.gsmseller.cart;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping("/add")
    public ResponseEntity<Cart> addToCart(Principal principal, @RequestParam String technicalId) {
        String username = principal.getName();
        Cart updated = cartService.addPhoneToCart(username, technicalId);
        return ResponseEntity.ok(updated);
    }

    @GetMapping
    public ResponseEntity<Cart> getCart(Principal principal) {
        String username = principal.getName();
        Cart cart = cartService.getCart(username);
        return ResponseEntity.ok(cart);
    }

    @DeleteMapping("/{technicalId}")
    public ResponseEntity<Cart> removeFromCart(Principal principal, @PathVariable String technicalId) {
        String username = principal.getName();
        Cart after = cartService.removeFromCart(username, technicalId);
        return ResponseEntity.ok(after);
    }
}

