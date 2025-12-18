package pl.gesieniec.gsmseller.user;

public record RegisterRequest(
    String email,
    String password,
    String confirmPassword
) {}
