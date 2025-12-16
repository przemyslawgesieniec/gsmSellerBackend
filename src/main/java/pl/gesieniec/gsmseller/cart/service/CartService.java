package pl.gesieniec.gsmseller.cart.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.gesieniec.gsmseller.cart.Cart;
import pl.gesieniec.gsmseller.cart.CartItem;
import pl.gesieniec.gsmseller.cart.CartItemDto;
import pl.gesieniec.gsmseller.cart.CartRepository;
import pl.gesieniec.gsmseller.common.ItemType;
import pl.gesieniec.gsmseller.phone.stock.PhoneStock;
import pl.gesieniec.gsmseller.phone.stock.PhoneStockDto;
import pl.gesieniec.gsmseller.phone.stock.PhoneStockService;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final PhoneStockService phoneStockService;

    @Transactional
    public Cart addPhoneToCart(String username, UUID technicalId) {
        Cart cart = getOrCreateCart(username);

        if (cart.getTechnicalIds().contains(technicalId)) {
            log.info("ℹ [{}] Telefon {} już jest w koszyku", username, technicalId);
            return cart;
        }

        // 🔒 TWARDY WARUNEK BIZNESOWY
        PhoneStock phone = phoneStockService
            .validateCanBeAddedToCart(technicalId, username);

        cart.addItem(CartItem.fromPhone(
            phoneStockService.getByTechnicalId(technicalId)
        ));

        log.info("➕ [{}] Dodano telefon {} do koszyka", username, technicalId);

        return cartRepository.save(cart);
    }


    public Cart getCart(String username) {
        return getOrCreateCart(username);
    }

    @Transactional
    public Cart removeFromCart(String username, UUID technicalId) {
        Cart cart = getOrCreateCart(username);

        if (cart.remove(technicalId)) {
            log.info("❌ [{}] Usunięto przedmiot {} z koszyka", username, technicalId);
        } else {
            log.warn("⚠ [{}] Przedmiotu {} nie był w koszyku", username, technicalId);
        }

        return cartRepository.save(cart);
    }


    @Transactional
    public Cart addMiscItem(String username, String description, BigDecimal price, UUID technicalId) {
        Cart cart = getOrCreateCart(username);

        if (cart.getTechnicalIds().contains(technicalId)) {
            log.info("ℹ [{}] Przedmiot {} już jest w koszyku. Zostanie zaktualizowany.", username, technicalId);
            cart.updateMiscItem( description,  price,  technicalId);
        }
        else{
            cart.addItem(new CartItem(description, price, technicalId, ItemType.MISC));
            log.info("➕ [{}] Dodano przedmiot {} do koszyka", username, technicalId);
        }
        return cartRepository.save(cart);
    }

    private Cart getOrCreateCart(String username) {
        return cartRepository.findByUsername(username)
            .orElseGet(() -> {
                log.info("🛠 [{}] Tworzę nowy koszyk", username);
                Cart c = new Cart();
                c.setUsername(username);
                return cartRepository.save(c);
            });
    }

    /**
     * ✔ Zwraca pełne dane telefonów z koszyka jako DTO
     */
    public List<PhoneStockDto> getPhonesInCart(String username) {
        Cart cart = getOrCreateCart(username);

        return cart.getPhonesIds().stream()
            .map(id -> {
                try {
                    return phoneStockService.getByTechnicalId(id);
                } catch (IllegalArgumentException e) {
                    log.error("❌ Błędny UUID w koszyku: {}", id);
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .toList();
    }

    public List<CartItemDto> getMiscInCart(String username) {
        Cart cart = getOrCreateCart(username);
        return cart.getMicsItems();
    }

    @Transactional
    public void clearCart(String username) {
        Cart cart = getOrCreateCart(username);

        if (cart.getItems().isEmpty()) {
            log.info("🧹 [{}] Koszyk już był pusty – nic do czyszczenia", username);
            return;
        }
        cart.clear();
        log.info("🧹 [{}] Wyczyszczono koszyk ({} pozycji)", username, cart.getItems().size());
    }

}
