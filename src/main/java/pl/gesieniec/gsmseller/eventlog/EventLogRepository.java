package pl.gesieniec.gsmseller.eventlog;

import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EventLogRepository extends JpaRepository<EventLogEntry, java.util.UUID>, JpaSpecificationExecutor<EventLogEntry> {
    long deleteByOccurredAtBefore(Instant cutoff);
}
