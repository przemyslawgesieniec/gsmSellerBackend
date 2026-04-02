package pl.gesieniec.gsmseller.offer.model.specs;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunicationSpecs {
    private String wifi;
    private String portType;
    private String bluetoothVersion;
}
