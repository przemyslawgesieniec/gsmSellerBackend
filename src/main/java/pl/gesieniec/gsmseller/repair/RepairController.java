package pl.gesieniec.gsmseller.repair;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.gesieniec.gsmseller.repair.model.RepairDto;
import pl.gesieniec.gsmseller.repair.model.RepairStatus;
import pl.gesieniec.gsmseller.repair.model.RestoreToShopRequest;

@Slf4j
@RestController
@RequestMapping("/api/v1/repairs")
@RequiredArgsConstructor
public class RepairController {

    private final RepairService service;

    @GetMapping
    public List<RepairDto> getAllRepairs() {
        return service.getAllRepairs();
    }

    @GetMapping("/{technicalId}")
    public RepairDto getRepair(@PathVariable UUID technicalId) {
        return service.getRepair(technicalId);
    }

    @PostMapping
    public RepairDto addRepair(@RequestBody RepairDto dto) {
        log.info("Dodawanie nowej naprawy: {}", dto);
        return service.addRepair(dto);
    }

    @PutMapping("/{technicalId}")
    public RepairDto updateRepair(@PathVariable UUID technicalId, @RequestBody RepairDto dto) {
        log.info("Aktualizacja naprawy {}: {}", technicalId, dto);
        return service.updateRepair(technicalId, dto);
    }

    @PatchMapping("/{technicalId}/status")
    public RepairDto updateStatus(@PathVariable UUID technicalId, @RequestParam RepairStatus status) {
        log.info("Zmiana statusu naprawy {} na {}", technicalId, status);
        return service.updateStatus(technicalId, status);
    }

    @GetMapping(value = "/{technicalId}/receipt", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getRepairReceipt(@PathVariable UUID technicalId) {
        log.info("Generowanie pokwitowania przyjęcia dla naprawy: {}", technicalId);
        byte[] pdf = service.generateReceiptPdf(technicalId);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=pokwitowanie_przyjecia_" + technicalId + ".pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }

    @GetMapping(value = "/{technicalId}/handover", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getRepairHandover(@PathVariable UUID technicalId) {
        log.info("Generowanie potwierdzenia odbioru dla naprawy: {}", technicalId);
        byte[] pdf = service.generateHandoverPdf(technicalId);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=potwierdzenie_odbioru_" + technicalId + ".pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }

    @PostMapping("/{technicalId}/restore-to-shop")
    public ResponseEntity<Void> restoreToShop(
        @PathVariable UUID technicalId,
        @RequestBody RestoreToShopRequest request
    ) {
        log.info("Przywracanie naprawionego telefonu na sklep: {}", technicalId);
        service.restoreToShop(technicalId, request);
        return ResponseEntity.ok().build();
    }
}
