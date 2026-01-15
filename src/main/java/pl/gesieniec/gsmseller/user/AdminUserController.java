package pl.gesieniec.gsmseller.user;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public List<UserDto> allUsers() {
        return userService.getAllUsers();
    }

    @PutMapping("/{id}/toggle-status")
    public void toggleStatus(@PathVariable Long id) {
        userService.toggleUserStatus(id);
    }
}
