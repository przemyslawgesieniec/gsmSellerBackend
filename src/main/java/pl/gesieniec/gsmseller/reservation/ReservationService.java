package pl.gesieniec.gsmseller.reservation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import pl.gesieniec.gsmseller.phone.stock.model.PhoneSoldEvent;
import pl.gesieniec.gsmseller.phone.stock.model.PhoneHandedOverEvent;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void createReservation(ReservationRequest request) {
        log.info("Creating reservation for phone: {}", request.technicalId());
        
        reservationRepository.findByTechnicalId(request.technicalId())
                .ifPresent(r -> {
                    if (r.getExpiryTime().isAfter(LocalDateTime.now())) {
                        throw new ReservationConflictException("Phone already reserved");
                    } else {
                        log.info("Removing expired reservation for phone: {} before creating new one", request.technicalId());
                        reservationRepository.delete(r);
                        eventPublisher.publishEvent(new ReservationExpiredEvent(request.technicalId(), false));
                    }
                });

        Reservation reservation = new Reservation(
                request.phoneNumber(),
                request.name(),
                request.technicalId()
        );

        reservationRepository.save(reservation);
        eventPublisher.publishEvent(new ReservationCreatedEvent(request.technicalId(), true));
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void removeExpiredReservations() {
        LocalDateTime now = LocalDateTime.now();
        List<Reservation> expired = reservationRepository.findAllByExpiryTimeBefore(now);
        
        if (!expired.isEmpty()) {
            log.info("Removing {} expired reservations", expired.size());
            expired.forEach(r -> {
                reservationRepository.delete(r);
                eventPublisher.publishEvent(new ReservationExpiredEvent(r.getTechnicalId(), false));
            });
        }
    }

    @Transactional
    public void cancelReservation(UUID technicalId, String canceledBy) {
        log.warn("Employee {} is manually removing reservation for phone: {}", canceledBy, technicalId);
        reservationRepository.findByTechnicalId(technicalId).ifPresent(reservation -> {
            reservationRepository.delete(reservation);
            eventPublisher.publishEvent(new ReservationCancelledEvent(technicalId, false, canceledBy));
        });
    }

    public ReservationStatus getReservationStatus(UUID technicalId) {
        return reservationRepository.findByTechnicalId(technicalId)
                .map(r -> new ReservationStatus(
                        r.getPhoneNumber(),
                        r.getName(),
                        r.getExpiryTime()
                ))
                .orElse(null);
    }

    @EventListener
    @Transactional
    public void onPhoneSold(PhoneSoldEvent event) {
        log.info("Removing reservation for sold phone: {}", event.technicalId());
        removeReservation(event.technicalId(), "PHONE_SOLD");
    }

    @EventListener
    @Transactional
    public void onPhoneHandedOver(PhoneHandedOverEvent event) {
        log.info("Removing reservation for handed over phone: {}", event.technicalId());
        removeReservation(event.technicalId(), "PHONE_HANDED_OVER");
    }

    private void removeReservation(UUID technicalId, String reason) {
        reservationRepository.findByTechnicalId(technicalId).ifPresent(reservation -> {
            reservationRepository.delete(reservation);
            eventPublisher.publishEvent(new ReservationCancelledEvent(technicalId, false, reason));
        });
    }
}
