package pl.gesieniec.gsmseller.phone.stock;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping
    public Page<PhoneStockDto> getPhones(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String model,
        @RequestParam(required = false) String imei1,
        @RequestParam(required = false) String imei2,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size
    ) {
       return service.getPhones(name, model, imei1, imei2, page, size);
    }

    @GetMapping("/test")
    public PhoneScanDto getTestPhone(){
        return new PhoneScanDto("Iphone","12GB","128",
            "black",
            "123dfwdfwef",
            "asdfwe234f");
    }
}
