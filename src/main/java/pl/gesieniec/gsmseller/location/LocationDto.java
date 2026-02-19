package pl.gesieniec.gsmseller.location;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LocationDto {
    private UUID technicalId;
    private String name;
    private String city;
    private String phoneNumber;
}
