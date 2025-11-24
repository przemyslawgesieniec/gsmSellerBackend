package pl.gesieniec.gsmseller.phone.stock;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.gesieniec.gsmseller.phone.scan.PhoneScanDto;

@Slf4j
@RestController
@RequestMapping("/api/v1/phones")
public class PhoneStockController {

    private final PhoneStockService service;

    public PhoneStockController(PhoneStockService service) {
        this.service = service;
    }

    @PostMapping
    public PhoneStockDto addPhone(@RequestBody PhoneScanDto phoneScanDto) {
        return service.savePhone(phoneScanDto);
    }

    @GetMapping
    public Page<PhoneStockDto> getPhones(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String model,
        @RequestParam(required = false) String imei,
        @RequestParam(required = false) String color,
        @RequestParam(required = false) Status status,
        @RequestParam(required = false) BigDecimal priceMin,
        @RequestParam(required = false) BigDecimal priceMax,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size
    ) {
        return service.getPhones(name, model, color, imei, status, priceMin, priceMax, page, size);
    }


    @PatchMapping("/{technicalId}")
    public PhoneStockDto updatePhone(
        @PathVariable UUID technicalId,
        @RequestBody PhoneStockDto updateDto) {

        log.info("Aktualizacja pozycji {} z danymi {}",technicalId,updateDto);
        PhoneStockDto phoneStockDto = service.updatePhone(technicalId, updateDto);
        log.info("Aktualizacja pozycji {} zrealizowana", technicalId);
        return phoneStockDto;
    }

    @GetMapping("/test")
    public PhoneScanDto getTestPhone() {
        return new PhoneScanDto("buba", "12GB", "128",
            "black",
            "123dfwdfwef");
    }
}
