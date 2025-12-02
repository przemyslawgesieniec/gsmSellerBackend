package pl.gesieniec.gsmseller.cart;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.gesieniec.gsmseller.phone.stock.PhoneStockDto;
import pl.gesieniec.gsmseller.phone.stock.PhoneStockService;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final PhoneStockService phoneStockService;

    private Cart getOrCreateCart(String username) {
        return cartRepository.findByUsername(username)
            .orElseGet(() -> {
                log.info("🛠 [{}] Tworzę nowy koszyk", username);
                Cart c = new Cart();
                c.setUsername(username);
                return cartRepository.save(c);
            });
    }

    @Transactional
    public Cart addPhoneToCart(String username, String technicalId) {
        Cart cart = getOrCreateCart(username);

        if (!cart.getPhoneIds().contains(technicalId)) {
            cart.addPhone(technicalId);
            log.info("➕ [{}] Dodano telefon {} do koszyka", username, technicalId);
        } else {
            log.info("ℹ [{}] Telefon {} już jest w koszyku", username, technicalId);
        }

        return cartRepository.save(cart);
    }

    public Cart getCart(String username) {
        return getOrCreateCart(username);
    }

    @Transactional
    public Cart removeFromCart(String username, String technicalId) {
        Cart cart = getOrCreateCart(username);

        if (cart.getPhoneIds().remove(technicalId)) {
            log.info("❌ [{}] Usunięto telefon {} z koszyka", username, technicalId);
        } else {
            log.warn("⚠ [{}] Telefon {} nie był w koszyku", username, technicalId);
        }

        return cartRepository.save(cart);
    }

    /**
     * ✔ Zwraca pełne dane telefonów z koszyka jako DTO
     */
    public List<PhoneStockDto> getPhonesInCart(String username) {
        Cart cart = getOrCreateCart(username);

        return cart.getPhoneIds().stream()
            .map(id -> {
                try {
                    return phoneStockService.getByTechnicalId(UUID.fromString(id));
                } catch (IllegalArgumentException e) {
                    log.error("❌ Błędny UUID w koszyku: {}", id);
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .toList();
    }
}
