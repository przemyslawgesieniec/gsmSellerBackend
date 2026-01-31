package pl.gesieniec.gsmseller.repair.model;

import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RepairClientDto {
    private UUID technicalId;
    private String name;
    private String surname;
    private String phoneNumber;
}
