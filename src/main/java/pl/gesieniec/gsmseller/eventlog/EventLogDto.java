package pl.gesieniec.gsmseller.eventlog;

import java.time.Instant;
import java.util.UUID;

public record EventLogDto(
    UUID id,
    Instant occurredAt,
    EventLogType eventType,
    EventLogOutcome outcome,
    String actorUsername,
    String imei,
    String httpMethod,
    String requestPath,
    String details
) {
    static EventLogDto from(EventLogEntry entry) {
        return new EventLogDto(
            entry.getId(), entry.getOccurredAt(), entry.getEventType(), entry.getOutcome(),
            entry.getActorUsername(), entry.getImei(), entry.getHttpMethod(), entry.getRequestPath(), entry.getDetails()
        );
    }
}
