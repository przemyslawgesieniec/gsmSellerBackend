package pl.gesieniec.gsmseller.location;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "locations")
@Getter
@Setter
@NoArgsConstructor
public class LocationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID technicalId;

    @Column(nullable = false)
    private String name;

    private String city;
    private String phoneNumber;

    public LocationEntity(String name, String city, String phoneNumber) {
        this.technicalId = UUID.randomUUID();
        this.name = name;
        this.city = city;
        this.phoneNumber = phoneNumber;
    }
}
