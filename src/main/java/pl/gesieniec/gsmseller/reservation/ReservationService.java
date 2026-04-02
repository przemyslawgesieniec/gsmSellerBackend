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
                    throw new IllegalStateException("Phone already reserved");
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
                eventPublisher.publishEvent(new ReservationCreatedEvent(r.getTechnicalId(), false));
            });
        }
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
}
