package pl.gesieniec.gsmseller.eventlog;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class EventLogListener {

    private final EventLogRepository eventLogRepository;

    @EventListener
    public void persist(EventLogRecordedEvent event) {
        eventLogRepository.save(new EventLogEntry(event));
    }
}
