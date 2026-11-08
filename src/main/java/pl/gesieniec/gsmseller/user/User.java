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
    private String role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "location_id")
    private LocationEntity location;

    /**
     * 0 is the existing application size; the remaining values map to the three
     * larger accessibility levels exposed in the settings screen.
     */
    @Column(nullable = false, columnDefinition = "integer default 0")
    private int fontSizeLevel = 0;

    public User(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.status = UserStatus.INACTIVE;
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
    }

    public void deactivate() {
        if ("ROLE_ADMIN".equals(this.role)) {
            throw new IllegalStateException("Nie można dezaktywować administratora");
        }
        this.status = UserStatus.INACTIVE;
    }

    public void enableDynamicLocation() {
        if (!"ROLE_SELLER".equals(this.role)) return;
        this.role = "ROLE_DYNAMIC_LOCATION_SELLER";
    }

    public void disableDynamicLocation() {
        if (!"ROLE_DYNAMIC_LOCATION_SELLER".equals(this.role)) return;
        this.role = "ROLE_SELLER";
    }
}
