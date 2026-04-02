package pl.gesieniec.gsmseller.reservation;

import java.time.LocalDateTime;

public record ReservationStatus(
    String phoneNumber,
    String name,
    LocalDateTime expiryTime
) {}
