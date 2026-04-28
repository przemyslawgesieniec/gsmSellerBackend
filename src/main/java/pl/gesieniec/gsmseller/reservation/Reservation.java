package pl.gesieniec.gsmseller.reservation;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String phoneNumber;
    private String name;
    private UUID technicalId;
    private ZonedDateTime expiryTime;

    public Reservation(String phoneNumber, String name, UUID technicalId) {
        this.phoneNumber = phoneNumber;
        this.name = name;
        this.technicalId = technicalId;
        this.expiryTime = ZonedDateTime.now().plusHours(4);
    }
}
