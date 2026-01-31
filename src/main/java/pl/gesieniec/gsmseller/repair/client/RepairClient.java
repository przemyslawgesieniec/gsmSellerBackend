package pl.gesieniec.gsmseller.repair.client;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
@Table(name = "repair_clients")
public class RepairClient {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(unique = true, nullable = false)
    private UUID technicalId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String surname;

    @Column(nullable = false)
    private String phoneNumber;

    private RepairClient(String name, String surname, String phoneNumber) {
        this.technicalId = UUID.randomUUID();
        this.name = name;
        this.surname = surname;
        this.phoneNumber = phoneNumber;
    }

    public static RepairClient create(String name, String surname, String phoneNumber) {
        return new RepairClient(name, surname, phoneNumber);
    }

    public void update(String name, String surname, String phoneNumber) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (surname != null && !surname.isBlank()) {
            this.surname = surname;
        }
        if (phoneNumber != null && !phoneNumber.isBlank()) {
            this.phoneNumber = phoneNumber;
        }
    }
}
