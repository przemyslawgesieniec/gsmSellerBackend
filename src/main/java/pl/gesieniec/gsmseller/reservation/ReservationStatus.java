package pl.gesieniec.gsmseller.reservation;

import java.time.ZonedDateTime;

public record ReservationStatus(
    String phoneNumber,
    String name,
    ZonedDateTime expiryTime
) {}
