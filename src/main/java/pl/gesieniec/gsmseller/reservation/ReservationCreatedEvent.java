package pl.gesieniec.gsmseller.reservation;

import java.util.UUID;

public record ReservationCreatedEvent(
    UUID technicalId,
    boolean reserved
) {}
