package pl.gesieniec.gsmseller.repair.client;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import pl.gesieniec.gsmseller.repair.model.RepairClientDto;

@RestController
@RequestMapping("/api/v1/repair-clients")
@RequiredArgsConstructor
public class RepairClientController {

    private final RepairClientService service;

    @GetMapping
    public Page<RepairClientDto> getClients(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String surname,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "surname") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir) {
        return service.getClients(name, surname, phoneNumber, page, size, sortBy, sortDir);
    }

    @GetMapping("/search")
    public Page<RepairClientDto> searchClients(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.searchClients(query, page, size);
    }

    @PostMapping
    public RepairClientDto createClient(@RequestBody RepairClientDto dto) {
        return service.createClient(dto);
    }
}
