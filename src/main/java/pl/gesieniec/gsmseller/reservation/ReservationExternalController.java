package pl.gesieniec.gsmseller.reservation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/external/reservations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ReservationExternalController {

    private final ReservationService reservationService;

    @PostMapping
    public void createReservation(@RequestBody ReservationRequest request) {
        log.info("Creating external reservation for technicalId: {}", request.technicalId());
        reservationService.createReservation(request);
    }

    @GetMapping("/status/{technicalId}")
    public ReservationStatus getStatus(@PathVariable UUID technicalId) {
        log.info("Fetching external reservation status for technicalId: {}", technicalId);
        return reservationService.getReservationStatus(technicalId);
    }
}
