package pl.gesieniec.gsmseller.phone.scan;

import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import pl.gesieniec.gsmseller.phone.stock.model.PurchaseType;

@RestController
@RequestMapping("/api/v1/phones")
public class PhoneScanController {

    private final PhoneScanService phoneScanService;

    public PhoneScanController(PhoneScanService phoneScanService) {
        this.phoneScanService = phoneScanService;
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public List<PhoneScanDto> uploadFiles(
        @RequestParam("name") String name,
        @RequestParam("initialPrice") String initialPrice,
        @RequestParam("sellingPrice") String sellingPrice,
        @RequestParam("source") String source,
        @RequestParam("purchaseType") PurchaseType purchaseType,
        @RequestParam("description") String description,
        @RequestParam("batteryCondition") String batteryCondition,
        @RequestParam("used") boolean used,
        @RequestParam("photos") List<MultipartFile> photos
    ) {
        return phoneScanService.getPhoneScanDtos(name, source, initialPrice, sellingPrice, photos, purchaseType,
            description, batteryCondition, used);
    }

}
