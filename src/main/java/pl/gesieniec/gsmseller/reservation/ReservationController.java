package pl.gesieniec.gsmseller.reservation;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    public void createReservation(@RequestBody ReservationRequest request) {
        reservationService.createReservation(request);
    }

    @GetMapping("/status/{technicalId}")
    public ReservationStatus getStatus(@PathVariable UUID technicalId) {
        return reservationService.getReservationStatus(technicalId);
    }
}
