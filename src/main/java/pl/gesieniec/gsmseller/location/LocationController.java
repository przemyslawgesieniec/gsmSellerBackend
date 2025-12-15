package pl.gesieniec.gsmseller.location;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @PostMapping
    public ResponseEntity<LocationEntity> create(@RequestBody LocationRequest request) {
        return ResponseEntity.ok(locationService.createLocation(request));
    }

    @PutMapping("/{technicalId}")
    public ResponseEntity<LocationEntity> update(
        @PathVariable UUID technicalId,
        @RequestBody LocationRequest request) {

        return ResponseEntity.ok(locationService.updateLocation(technicalId, request));
    }

    @GetMapping
    public ResponseEntity<List<String>> getAllLocations() {
        return ResponseEntity.ok(locationService.getAllLocationNames());
    }


}

