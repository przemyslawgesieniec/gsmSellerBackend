package pl.gesieniec.gsmseller.eventlog;

import java.time.Instant;
import java.util.UUID;

/** Internal Spring application event. It intentionally contains no request body or secrets. */
public record EventLogRecordedEvent(
    Instant occurredAt,
    EventLogType eventType,
    EventLogOutcome outcome,
    String actorUsername,
    String imei,
    String httpMethod,
    String requestPath,
    String details,
    UUID resourceTechnicalId
) {
}
