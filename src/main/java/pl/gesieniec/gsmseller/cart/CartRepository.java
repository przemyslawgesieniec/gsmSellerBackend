package pl.gesieniec.gsmseller.cart;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.gesieniec.gsmseller.user.User;

public interface CartRepository extends JpaRepository<Cart, Long> {
}