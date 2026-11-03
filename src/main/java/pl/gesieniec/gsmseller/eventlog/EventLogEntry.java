package pl.gesieniec.gsmseller.eventlog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "event_log", indexes = {
    @Index(name = "idx_event_log_occurred_at", columnList = "occurredAt"),
    @Index(name = "idx_event_log_type", columnList = "eventType"),
    @Index(name = "idx_event_log_actor", columnList = "actorUsername"),
    @Index(name = "idx_event_log_imei", columnList = "imei")
})
public class EventLogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private Instant occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 80)
    private EventLogType eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventLogOutcome outcome;

    @Column(length = 255)
    private String actorUsername;

    @Column(length = 32)
    private String imei;

    @Column(nullable = false, length = 10)
    private String httpMethod;

    @Column(nullable = false, length = 500)
    private String requestPath;

    @Column(length = 1000)
    private String details;

    EventLogEntry(EventLogRecordedEvent event) {
        this.occurredAt = event.occurredAt();
        this.eventType = event.eventType();
        this.outcome = event.outcome();
        this.actorUsername = event.actorUsername();
        this.imei = event.imei();
        this.httpMethod = event.httpMethod();
        this.requestPath = event.requestPath();
        this.details = event.details();
    }
}
