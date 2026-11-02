package pl.gesieniec.gsmseller.notification;

import java.time.ZonedDateTime;
import java.util.UUID;

public record NotificationDto(
    Long id,
    NotificationType type,
    String title,
    String content,
    ZonedDateTime createdAt,
    ZonedDateTime readAt,
    UUID reservationTechnicalId,
    String reservationImei
) {
}
