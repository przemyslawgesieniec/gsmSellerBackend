package pl.gesieniec.gsmseller.user;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
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

    @PutMapping("/{id}/active")
    public void setActive(
        @PathVariable Long id,
        @RequestBody ActiveRequest req
    ) {
        userService.setUserActive(id, req.active());
    }

    @PutMapping("/{id}/dynamic-location")
    public void setDynamicLocation(
        @PathVariable Long id,
        @RequestBody DynamicLocationRequest req
    ) {
        userService.setDynamicLocation(id, req.enabled());
    }

}
