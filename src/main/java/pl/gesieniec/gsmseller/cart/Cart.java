package pl.gesieniec.gsmseller.cart;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Jeden koszyk na użytkownika
    @Column(nullable = false, unique = true)
    private String username;

    @ElementCollection
    @CollectionTable(name = "cart_items", joinColumns = @JoinColumn(name = "cart_id"))
    @Column(name = "phone_technical_id")
    private List<String> phoneIds = new ArrayList<>();

    public void addPhone(String technicalId) {
        phoneIds.add(technicalId);
    }

    public void removePhone(String technicalId) {
        phoneIds.remove(technicalId);
    }
}
