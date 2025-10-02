package pl.gesieniec.gsmseller.phone.stock;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.gesieniec.gsmseller.phone.scan.PhoneScanDto;

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

}
