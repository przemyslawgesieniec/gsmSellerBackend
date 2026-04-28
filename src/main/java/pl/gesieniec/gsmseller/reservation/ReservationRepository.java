package pl.gesieniec.gsmseller.reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findAllByExpiryTimeBefore(ZonedDateTime now);
    Optional<Reservation> findByTechnicalId(UUID technicalId);
}
