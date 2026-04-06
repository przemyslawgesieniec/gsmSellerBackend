package pl.gesieniec.gsmseller.purchase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/external/purchase")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PurchaseExternalController {

    private final PurchaseService purchaseService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public void createPurchase(
            @RequestParam("phoneModel") String phoneModel,
            @RequestParam("phoneNumber") String phoneNumber,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "photos", required = false) List<MultipartFile> photos
    ) {
        log.info("Received external purchase request for model: {}", phoneModel);
        purchaseService.createPurchase(phoneModel, phoneNumber, description, photos);
    }
}
