package pl.gesieniec.gsmseller.repair.servicepoint;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
@Table(name = "repair_service_points")
public class RepairServicePoint {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(unique = true, nullable = false)
    private UUID technicalId;

    @Column(unique = true, nullable = false)
    private String name;

    private RepairServicePoint(String name) {
        this.technicalId = UUID.randomUUID();
        this.name = normalizeName(name);
    }

    public static RepairServicePoint create(String name) {
        return new RepairServicePoint(name);
    }

    private static String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Nazwa punktu serwisowego jest wymagana");
        }
        return name.trim().replaceAll("\\s+", " ");
    }
}
