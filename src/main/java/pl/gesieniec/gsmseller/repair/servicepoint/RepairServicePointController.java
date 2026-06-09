package pl.gesieniec.gsmseller.repair.servicepoint;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/repair-service-points")
@RequiredArgsConstructor
public class RepairServicePointController {

    private final RepairServicePointService service;

    @GetMapping
    public List<RepairServicePointDto> search(@RequestParam(required = false) String query) {
        return service.search(query);
    }

    @PostMapping
    public RepairServicePointDto create(@RequestBody RepairServicePointDto dto) {
        return service.create(dto.name());
    }
}
