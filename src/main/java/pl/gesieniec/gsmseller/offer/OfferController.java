package pl.gesieniec.gsmseller.offer;

import kotlin.SinceKotlin;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pl.gesieniec.gsmseller.offer.model.OfferRequest;
import pl.gesieniec.gsmseller.offer.model.PhoneOffer;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/offers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OfferController {

    private final OfferService offerService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PhoneOffer createOffer(
        @RequestParam UUID phoneStockTechnicalId,
        @RequestParam(required = false) String screenSize,
        @RequestParam(required = false) String batteryCapacity,
        @RequestParam(required = false) String screenType,
        @RequestParam(required = false) List<MultipartFile> photoFiles
    ) {
        OfferRequest request = OfferRequest.builder()
            .phoneStockTechnicalId(phoneStockTechnicalId)
            .screenSize(screenSize)
            .batteryCapacity(batteryCapacity)
            .screenType(screenType)
            .build();
        return offerService.createOffer(request, photoFiles);
    }

    @PutMapping(value = "/{technicalId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PhoneOffer updateOffer(
        @PathVariable UUID technicalId,
        @RequestParam(required = false) String screenSize,
        @RequestParam(required = false) String batteryCapacity,
        @RequestParam(required = false) String screenType,
        @RequestParam(required = false) List<MultipartFile> photoFiles,
        @RequestParam(required = false) List<String> existingPhotos
    ) {
        OfferRequest request = OfferRequest.builder()
            .screenSize(screenSize)
            .batteryCapacity(batteryCapacity)
            .screenType(screenType)
            .photos(existingPhotos)
            .build();
        return offerService.updateOffer(technicalId, request, photoFiles);
    }

    @GetMapping("/available-phones")
    public Page<pl.gesieniec.gsmseller.phone.stock.model.PhoneStockDto> getAvailablePhones(
        @RequestParam(required = false) String search,
        @PageableDefault(sort = "createDateTime", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return offerService.getAvailablePhones(search, pageable);
    }
}
