package pl.gesieniec.gsmseller.user;

import java.security.Principal;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.gesieniec.gsmseller.location.LocationEntity;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserSettingsController {

    private final UserService userService;

    @PutMapping("/location/{technicalId}")
    public ResponseEntity<Void> assignLocation(
        @PathVariable UUID technicalId,
        Principal principal
    ) {
        userService.assignUserToLocation(principal.getName(), technicalId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/auth/me")
    public UserDto me(Authentication auth) {

        User user = userService.getUserByUsername(auth.getName())
            .orElseThrow();

        String location = Optional.ofNullable(user.getLocation())
            .map(LocationEntity::getName)
            .orElse(null);

        return new UserDto(
            user.getId(),
            user.getUsername(),
            user.getStatus(),
            user.getRole(),
            location
        );
    }
}
