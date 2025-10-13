package pl.gesieniec.gsmseller.phone.scan;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/phones")
public class PhoneScanController {

    private final PhoneScanAiService phoneScanAiService;

    public PhoneScanController(PhoneScanAiService phoneScanAiService) {
        this.phoneScanAiService = phoneScanAiService;
    }


}
