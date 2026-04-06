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

    @DeleteMapping("/{technicalId}")
    public void cancelReservation(@PathVariable UUID technicalId) {
        String username = "SYSTEM_USER";
        try {
            username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
        }
        reservationService.cancelReservation(technicalId, username);
    }
}
