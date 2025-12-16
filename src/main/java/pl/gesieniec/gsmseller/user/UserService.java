package pl.gesieniec.gsmseller.user;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.gesieniec.gsmseller.location.LocationEntity;
import pl.gesieniec.gsmseller.location.LocationService;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final LocationService locationService;

    @Transactional
    public void assignUserToLocation(String username, UUID technicalId) {

        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));

        LocationEntity location = locationService.getLocationByTechnicalId(technicalId);

        user.setLocation(location);

        userRepository.save(user);
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}
