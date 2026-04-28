package pl.gesieniec.gsmseller.reservation;

import java.util.UUID;

public record ReservationExpiredEvent(
    UUID technicalId,
    Boolean reserved
) {}
