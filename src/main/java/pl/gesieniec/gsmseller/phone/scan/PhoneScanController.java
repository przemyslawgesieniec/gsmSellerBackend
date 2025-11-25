package pl.gesieniec.gsmseller.phone.scan;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
        @RequestParam("price") String price,
        @RequestParam("source") String source,
        @RequestParam("photos") List<MultipartFile> photos
    ) {
        return phoneScanService.getPhoneScanDtos(name, source, price, photos);
    }

}
