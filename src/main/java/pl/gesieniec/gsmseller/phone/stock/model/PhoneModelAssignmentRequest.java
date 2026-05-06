package pl.gesieniec.gsmseller.phone.stock.model;

import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PhoneModelAssignmentRequest {
    private UUID phoneModelTechnicalId;
}
