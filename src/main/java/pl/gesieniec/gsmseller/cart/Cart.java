package pl.gesieniec.gsmseller.cart;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.gesieniec.gsmseller.common.ItemType;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "cart_id")
    private List<CartItem> items = new ArrayList<>();

    public void addItem(CartItem item) {
        items.add(item);
    }

    public List<UUID> getTechnicalIds() {
        return items
            .stream()
            .map(CartItem::getTechnicalId)
            .toList();
    }

    public boolean remove(UUID technicalId) {
        return items.stream()
            .filter(e -> e.getTechnicalId().equals(technicalId))
            .findAny()
            .map(items::remove)
            .orElse(false);
    }

    public List<UUID> getPhonesIds() {
        return items.stream()
            .filter(e -> e.getItemType().equals(ItemType.PHONE))
            .map(CartItem::getTechnicalId).toList();
    }


    public List<CartItemDto> getMicsItems() {
        return items.stream()
            .filter(e -> e.getItemType().equals(ItemType.MISC))
            .map(i -> new CartItemDto(i.getTechnicalId(), i.getDescription(), i.getPrice(), i.getItemType()))
            .toList();
    }

    public void updateMiscItem(String description, BigDecimal price, UUID technicalId) {
        items.stream()
            .filter(e->e.getTechnicalId().equals(technicalId))
            .findFirst()
            .ifPresent(e->e.update(description, price));
    }

    public void clear() {
        items.clear();
    }
}
