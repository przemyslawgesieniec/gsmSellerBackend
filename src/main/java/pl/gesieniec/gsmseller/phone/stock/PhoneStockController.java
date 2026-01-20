package pl.gesieniec.gsmseller.phone.stock;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.gesieniec.gsmseller.phone.scan.PhoneScanDto;
import pl.gesieniec.gsmseller.phone.stock.model.HandoverRequest;
import pl.gesieniec.gsmseller.phone.stock.model.PhoneStockDto;
import pl.gesieniec.gsmseller.phone.stock.model.Status;

@Slf4j
@RestController
@RequestMapping("/api/v1/phones")
public class PhoneStockController {

    private final PhoneStockService service;

    public PhoneStockController(PhoneStockService service) {
        this.service = service;
    }

    @PostMapping
    public void addPhones(@RequestBody List<PhoneScanDto> phoneScanDtoList, Principal principal) {
        log.info("Teleofny otrzymane przez {} do zapisu:  {} ", principal, phoneScanDtoList);
        service.saveAllPhone(phoneScanDtoList, principal.getName());
    }

    @GetMapping
    public Page<PhoneStockDto> getPhones(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String model,
        @RequestParam(required = false) String imei,
        @RequestParam(required = false) String color,
        @RequestParam(required = false) Status status,
        @RequestParam(required = false) String locationName,
        @RequestParam(required = false) BigDecimal priceMin,
        @RequestParam(required = false) BigDecimal priceMax,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size
    ) {
        return service.getPhones(name, model, color, imei, status,locationName, priceMin, priceMax, page, size);
    }


    @PatchMapping("/{technicalId}")
    public PhoneStockDto updatePhone(
        @PathVariable UUID technicalId,
        @RequestBody PhoneStockDto updateDto) {

        log.info("Aktualizacja pozycji {} z danymi {}", technicalId, updateDto);
        PhoneStockDto phoneStockDto = service.updatePhone(technicalId, updateDto);
        log.info("Aktualizacja pozycji {} zrealizowana", technicalId);
        return phoneStockDto;
    }

    @PostMapping("/{technicalId}/accept")
    public ResponseEntity<Void> acceptPhoneAtLocation(@PathVariable UUID technicalId, Principal principal) {
        service.acceptPhone(technicalId, principal.getName());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{technicalId}")
    public ResponseEntity<Void> removePhone(
        @PathVariable UUID technicalId
    ) {
        service.removePhone(technicalId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{technicalId}/remove-from-location")
    public ResponseEntity<Void> removePhoneFromLocation(
        @PathVariable UUID technicalId
    ) {
        service.removePhoneFromLocation(technicalId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{technicalId}/handover")
    public ResponseEntity<Void> handoverPhone(
        @PathVariable UUID technicalId,
        @RequestBody HandoverRequest request,
        Principal principal
    ) {
        service.handoverPhone(
            technicalId,
            request,
            principal.getName()
        );
        return ResponseEntity.ok().build();
    }


}
