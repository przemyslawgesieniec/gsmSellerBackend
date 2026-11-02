package pl.gesieniec.gsmseller.notification;

import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.gesieniec.gsmseller.user.User;
import pl.gesieniec.gsmseller.user.UserService;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    @GetMapping
    public List<NotificationDto> list(@RequestParam(defaultValue = "false") boolean unreadOnly, Principal principal) {
        return notificationService.listForUser(currentUser(principal).getId(), unreadOnly);
    }

    @GetMapping("/unread-count")
    public long unreadCount(Principal principal) {
        return notificationService.unreadCount(currentUser(principal).getId());
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id, Principal principal) {
        notificationService.markRead(id, currentUser(principal).getId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllRead(Principal principal) {
        notificationService.markAllRead(currentUser(principal).getId());
        return ResponseEntity.noContent().build();
    }

    private User currentUser(Principal principal) {
        return userService.getUserByUsername(principal.getName()).orElseThrow();
    }
}
