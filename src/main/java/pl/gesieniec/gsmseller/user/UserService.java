package pl.gesieniec.gsmseller.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.gesieniec.gsmseller.location.LocationEntity;
import pl.gesieniec.gsmseller.location.LocationService;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final LocationService locationService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void assignUserToLocation(String username, UUID technicalId) {

        log.info(username);
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));

        LocationEntity location = locationService.getLocationByTechnicalId(technicalId);

        user.setLocation(location);

        userRepository.save(user);
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public void register(RegisterRequest req) {
        if (!req.password().equals(req.confirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        if (userRepository.existsByUsername(req.email())) {
            throw new IllegalStateException("User exists");
        }

        User user = new User(
            req.email(),
            passwordEncoder.encode(req.password()),
            "ROLE_SELLER"
        );

        userRepository.save(user);
        log.info("Użytkownik {} zarejestrowany poprawnie. Wymaga aktywacji",user);
    }

    public void activateUser(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow();

        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
    }

    public List<UserDto> getInactiveUsers() {
        return userRepository.findByStatus(UserStatus.INACTIVE)
            .stream()
            .map(u -> new UserDto(u.getId(), u.getUsername()))
            .toList();
    }

}
