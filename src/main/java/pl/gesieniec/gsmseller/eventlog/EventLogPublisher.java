package pl.gesieniec.gsmseller.eventlog;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * The only entry point for publishing audit events. Keeping publishing separate
 * from persistence lets business code remain decoupled from the audit store.
 */
@Service
@RequiredArgsConstructor
public class EventLogPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public void publish(
        EventLogType eventType,
        EventLogOutcome outcome,
        String actorUsername,
        String imei,
        String httpMethod,
        String requestPath,
        String details
    ) {
        applicationEventPublisher.publishEvent(new EventLogRecordedEvent(
            Instant.now(), eventType, outcome, trim(actorUsername, 255), trim(imei, 32),
            trim(httpMethod, 10), trim(requestPath, 500), trim(details, 1000)
        ));
    }

    private String trim(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
