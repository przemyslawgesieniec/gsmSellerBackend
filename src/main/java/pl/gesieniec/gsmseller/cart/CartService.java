package pl.gesieniec.gsmseller.cart;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.gesieniec.gsmseller.phone.stock.PhoneStock;
import pl.gesieniec.gsmseller.phone.stock.PhoneStockRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final PhoneStockRepository phoneStockRepository;

    public Cart addPhoneToCart(String username, String phoneTechnicalId) {
//        Cart cart = cartRepository.findById(username).orElseGet(() -> {
//            Cart c = new Cart();
//            c.setOwnerUsername(username);
//            return c;
//        });
//
//        // sprawdź, czy telefon istnieje
//        Optional<PhoneStock> phone = phoneStockRepository.findByTechnicalId(phoneTechnicalId);
//        if (phone.isEmpty()) {
//            throw new IllegalArgumentException("Nie znaleziono telefonu o id: " + technicalId);
//        }
//
//        if (!cart.getTechnicalIds().contains(technicalId)) {
//            cart.getTechnicalIds().add(technicalId);
//        }
//
//        return cartRepository.save(cart);
        return null;
//        TODO implement me
    }

    public Cart getCart(String username) {
//        return cartRepository.findById(username).orElseGet(() -> {
//            Cart c = new Cart();
//            c.setOwnerUsername(username);
//            return c;
//        });
        return null;
    }

    public Cart removeFromCart(String username, String technicalId) {
//        Cart cart = cartRepository.findById(username).orElse(null);
//        if (cart == null) return new Cart(username, new java.util.ArrayList<>());
//        cart.getTechnicalIds().remove(technicalId);
//        return cartRepository.save(cart);
        return null;
    }
}
