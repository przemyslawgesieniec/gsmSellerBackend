package pl.gesieniec.gsmseller.eventlog;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventLogService {

    static final int RETENTION_DAYS = 30;

    private final EventLogRepository eventLogRepository;

    @Transactional(readOnly = true)
    public Page<EventLogDto> search(
        EventLogType eventType, String imei, String actorUsername, Instant from, Instant to, Pageable pageable
    ) {
        return eventLogRepository.findAll(EventLogSpecifications.matches(eventType, imei, actorUsername, from, to), pageable)
            .map(EventLogDto::from);
    }

    /** Removes data older than the required 30-day retention period every night. */
    @Scheduled(cron = "${event-log.retention-cron:0 10 0 * * *}")
    @Transactional
    public void purgeExpiredEntries() {
        long removed = eventLogRepository.deleteByOccurredAtBefore(Instant.now().minus(RETENTION_DAYS, ChronoUnit.DAYS));
        if (removed > 0) {
            log.info("Deleted {} expired event-log entries", removed);
        }
    }
}
