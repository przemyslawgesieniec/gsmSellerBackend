package pl.gesieniec.gsmseller.offer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    private final CloudflareImagesService cloudflareImagesService;

    @GetMapping("/photos/{id}")
    public ResponseEntity<Void> getPhoto(@PathVariable UUID id) {
        OfferPhoto photo = offerService.getPhotos(List.of(id)).stream().findFirst()
            .orElseThrow(() -> new pl.gesieniec.gsmseller.common.EntityNotFoundException("Photo not found: " + id));

        if (photo.getImageId() == null) {
            return ResponseEntity.notFound().build();
        }

        String imageUrl = cloudflareImagesService.getImageUrl(photo.getImageId(), "public");
        return ResponseEntity.status(HttpStatus.FOUND)
            .header(HttpHeaders.LOCATION, imageUrl)
            .build();
    }

    @GetMapping("/photos/{id}/thumbnail")
    public ResponseEntity<Void> getPhotoThumbnail(@PathVariable UUID id) {
        OfferPhoto photo = offerService.getPhotos(List.of(id)).stream().findFirst()
            .orElseThrow(() -> new pl.gesieniec.gsmseller.common.EntityNotFoundException("Photo not found: " + id));

        if (photo.getImageId() == null) {
            return ResponseEntity.notFound().build();
        }

        String imageUrl = cloudflareImagesService.getImageUrl(photo.getImageId(), "thumbnail");
        return ResponseEntity.status(HttpStatus.FOUND)
            .header(HttpHeaders.LOCATION, imageUrl)
            .build();
    }

    @GetMapping
    public Page<PhoneOffer> getOffers(
        @RequestParam(required = false) String brand,
        @RequestParam(required = false) String model,
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String imei,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String location,
        @RequestParam(required = false) BigDecimal minPrice,
        @RequestParam(required = false) BigDecimal maxPrice,
        @RequestParam(required = false) String search,
        @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Specification<Offer> spec = Specification.allOf(
            OfferSpecifications.hasBrand(brand),
            OfferSpecifications.hasName(name),
            OfferSpecifications.hasImei(imei),
            OfferSpecifications.hasStatus(status),
            OfferSpecifications.hasLocation(location),
            OfferSpecifications.hasPriceBetween(minPrice, maxPrice),
            OfferSpecifications.search(search)
        );

        log.info("Searching for internal offers: {}", spec);
        return offerService.getOffers(spec, pageable);
    }

    @GetMapping("/{technicalId}")
    public PhoneOffer getOffer(@PathVariable UUID technicalId) {
        return offerService.getOffer(technicalId);
    }

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
        @RequestParam(required = false) List<UUID> photos,
        @RequestParam(required = false) List<String> photoOrder
    ) {
        OfferRequest request = OfferRequest.builder()
            .photos(photos)
            .photoOrder(photoOrder)
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
