package pl.gesieniec.gsmseller.eventlog;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import pl.gesieniec.gsmseller.common.ItemType;
import pl.gesieniec.gsmseller.event.ItemsSoldEvent;
import pl.gesieniec.gsmseller.phone.stock.PhoneStockRepository;
import pl.gesieniec.gsmseller.reservation.ReservationExpiredEvent;

/**
 * Captures internal, non-HTTP business events as well (notably scheduled work).
 * Events emitted by a request are intentionally recorded with the responsible
 * employee carried by the domain event.
 */
@Component
@RequiredArgsConstructor
class DomainEventLogListener {

    private final EventLogPublisher eventLogPublisher;
    private final PhoneStockRepository phoneStockRepository;

    @EventListener
    public void phonesSold(ItemsSoldEvent event) {
        event.items().stream()
            .filter(item -> item.getItemType() == ItemType.PHONE && item.getTechnicalId() != null)
            .forEach(item -> eventLogPublisher.publish(
                EventLogType.PHONE_SOLD,
                EventLogOutcome.SUCCESS,
                event.username(),
                imeiFor(item.getTechnicalId()),
                "EVENT",
                "/internal/receipt/" + event.receiptNumber(),
                "Phone sold on receipt " + event.receiptNumber(),
                item.getTechnicalId()
            ));
    }

    @EventListener
    public void reservationExpired(ReservationExpiredEvent event) {
        eventLogPublisher.publish(
            EventLogType.RESERVATION_EXPIRED,
            EventLogOutcome.SUCCESS,
            "SYSTEM",
            imeiFor(event.technicalId()),
            "EVENT",
            "/internal/reservations/" + event.technicalId(),
            "Reservation expired automatically",
            event.technicalId()
        );
    }

    private String imeiFor(java.util.UUID technicalId) {
        return phoneStockRepository.findByTechnicalId(technicalId).map(phone -> phone.getImei()).orElse(null);
    }
}
