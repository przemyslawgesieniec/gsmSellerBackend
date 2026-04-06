package pl.gesieniec.gsmseller.reservation;

import java.util.UUID;

public record ReservationCancelledEvent(
    UUID technicalId,
    boolean reserved,
    String canceledBy
) {}
