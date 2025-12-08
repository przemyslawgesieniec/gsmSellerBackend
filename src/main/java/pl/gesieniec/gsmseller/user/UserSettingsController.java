package pl.gesieniec.gsmseller.user;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserSettingsController {

    private final UserService userService;

    @PutMapping("/{username}/location/{technicalId}")
    public ResponseEntity<Void> assignLocation(
        @PathVariable String username,
        @PathVariable UUID technicalId
    ) {
        userService.assignUserToLocation(username, technicalId);
        return ResponseEntity.ok().build();
    }
}
