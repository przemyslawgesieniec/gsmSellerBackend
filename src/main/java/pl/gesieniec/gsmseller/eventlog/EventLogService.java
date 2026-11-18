package pl.gesieniec.gsmseller.eventlog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventLogService {

    static final int RETENTION_DAYS = 30;

    private final EventLogRepository eventLogRepository;

    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordRepairDeletion(String actor, UUID technicalId, String imei, String rma, Object snapshot) {
        EventLogRecordedEvent event = new EventLogRecordedEvent(
            Instant.now(), EventLogType.REPAIR_UPDATED, EventLogOutcome.SUCCESS, actor, imei,
            "DELETE", "/api/v1/repairs/" + technicalId, rma, technicalId
        );
        try {
            eventLogRepository.save(EventLogEntry.repairDeletion(event, objectMapper.writeValueAsString(snapshot)));
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException("Nie udało się zapisać danych usuwanej naprawy w audycie", e);
        }
    }

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
