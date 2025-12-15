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


    public LocationEntity(String name) {
        this.technicalId = UUID.randomUUID();
        this.name = name;
    }
}
