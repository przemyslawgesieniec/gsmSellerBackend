package pl.gesieniec.gsmseller.location;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository locationRepository;

    public LocationEntity createLocation(LocationRequest request) {
        LocationEntity locationEntity = new LocationEntity(request.getName(), request.getCity(), request.getPhoneNumber());
        return locationRepository.save(locationEntity);
    }

    public LocationEntity updateLocation(UUID technicalId, LocationRequest request) {
        LocationEntity location = getLocationByTechnicalId(technicalId);

        location.setName(request.getName());
        location.setCity(request.getCity());
        location.setPhoneNumber(request.getPhoneNumber());

        return locationRepository.save(location);
    }

    public LocationEntity getLocationByTechnicalId(UUID technicalId) {
        return locationRepository.findByTechnicalId(technicalId)
            .orElseThrow(() -> new RuntimeException("Location not found"));
    }

    public List<LocationDto> getAllLocations() {
        return locationRepository.findAll()
            .stream()
            .map(l -> new LocationDto(
                l.getTechnicalId(),
                l.getName(),
                l.getCity(),
                l.getPhoneNumber()
            ))
            .toList();
    }


}
