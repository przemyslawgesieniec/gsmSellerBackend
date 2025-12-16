package pl.gesieniec.gsmseller.location;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class LocationRequest {
    private String name;
    private String address;
    private String city;
}
