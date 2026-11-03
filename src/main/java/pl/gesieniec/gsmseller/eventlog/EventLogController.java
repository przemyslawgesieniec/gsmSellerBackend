package pl.gesieniec.gsmseller.eventlog;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/event-log")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@RequiredArgsConstructor
public class EventLogController {

    private final EventLogService eventLogService;

    @GetMapping
    public Page<EventLogDto> search(
        @RequestParam(required = false) EventLogType eventType,
        @RequestParam(required = false) String imei,
        @RequestParam(required = false) String actorUsername,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
        @PageableDefault(sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return eventLogService.search(eventType, imei, actorUsername, from, to, pageable);
    }

    @GetMapping("/types")
    public List<EventLogType> types() {
        return Arrays.asList(EventLogType.values());
    }
}
