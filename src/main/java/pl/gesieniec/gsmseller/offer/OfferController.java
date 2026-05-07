package pl.gesieniec.gsmseller.offer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pl.gesieniec.gsmseller.offer.model.OfferRequest;
import pl.gesieniec.gsmseller.offer.model.PhoneOffer;

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
        @RequestParam(required = false) List<MultipartFile> photoFiles
    ) {
        OfferRequest request = OfferRequest.builder()
            .phoneStockTechnicalId(phoneStockTechnicalId)
            .build();
        return offerService.createOffer(request, photoFiles);
    }

    @PutMapping(value = "/{technicalId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PhoneOffer updateOffer(
        @PathVariable UUID technicalId,
        @RequestParam(required = false) List<MultipartFile> photoFiles,
        @RequestParam(required = false) List<UUID> photos
    ) {
        OfferRequest request = OfferRequest.builder()
            .photos(photos)
            .build();
        return offerService.updateOffer(technicalId, request, photoFiles);
    }

    @DeleteMapping("/{technicalId}")
    public void deleteOffer(@PathVariable UUID technicalId) {
        offerService.deleteOffer(technicalId);
    }

    @GetMapping("/available-phones")
    public Page<pl.gesieniec.gsmseller.phone.stock.model.PhoneStockDto> getAvailablePhones(
        @RequestParam(required = false) String search,
        @PageableDefault(sort = "createDateTime", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return offerService.getAvailablePhones(search, pageable);
    }
}
