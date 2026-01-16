package pl.gesieniec.gsmseller.user;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String username;
    private UserStatus status;
    private String role;
    private String location;
}
