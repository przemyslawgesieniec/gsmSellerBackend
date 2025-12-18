package pl.gesieniec.gsmseller.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import pl.gesieniec.gsmseller.cart.Cart;
import pl.gesieniec.gsmseller.location.LocationEntity;
@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
@Table(name = "app_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username; // email

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role; // ROLE_ADMIN / ROLE_SELLER

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @OneToOne
    @JoinColumn(name = "location_id")
    private LocationEntity location;

    public User(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.status = UserStatus.INACTIVE; // ⬅️ domyślnie NIEAKTYWNY
    }
}
