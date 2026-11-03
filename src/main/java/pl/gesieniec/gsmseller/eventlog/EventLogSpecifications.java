package pl.gesieniec.gsmseller.eventlog;

import java.time.Instant;
import org.springframework.data.jpa.domain.Specification;

final class EventLogSpecifications {

    private EventLogSpecifications() {
    }

    static Specification<EventLogEntry> matches(
        EventLogType eventType, String imei, String actorUsername, Instant from, Instant to
    ) {
        return Specification.where(hasType(eventType))
            .and(hasImei(imei))
            .and(hasActor(actorUsername))
            .and(occurredAfter(from))
            .and(occurredBefore(to));
    }

    private static Specification<EventLogEntry> hasType(EventLogType eventType) {
        return (root, query, cb) -> eventType == null ? cb.conjunction() : cb.equal(root.get("eventType"), eventType);
    }

    private static Specification<EventLogEntry> hasImei(String imei) {
        return (root, query, cb) -> isBlank(imei) ? cb.conjunction()
            : cb.like(cb.lower(root.get("imei")), contains(imei));
    }

    private static Specification<EventLogEntry> hasActor(String actorUsername) {
        return (root, query, cb) -> isBlank(actorUsername) ? cb.conjunction()
            : cb.like(cb.lower(root.get("actorUsername")), contains(actorUsername));
    }

    private static Specification<EventLogEntry> occurredAfter(Instant from) {
        return (root, query, cb) -> from == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("occurredAt"), from);
    }

    private static Specification<EventLogEntry> occurredBefore(Instant to) {
        return (root, query, cb) -> to == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("occurredAt"), to);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String contains(String value) {
        return "%" + value.trim().toLowerCase() + "%";
    }
}
