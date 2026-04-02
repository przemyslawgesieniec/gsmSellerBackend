package pl.gesieniec.gsmseller.reservation;

import java.util.UUID;

public record ReservationRequest(
    String phoneNumber,
    String name,
    UUID technicalId
) {}
