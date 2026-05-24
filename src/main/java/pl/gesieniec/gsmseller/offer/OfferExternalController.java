package pl.gesieniec.gsmseller.offer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.gesieniec.gsmseller.offer.model.PublicPhoneOffer;
import pl.gesieniec.gsmseller.phone.model.PhoneModelFilterOption;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import pl.gesieniec.gsmseller.phone.model.PhoneModelsService;

@Slf4j
@RestController
@RequestMapping("/api/v1/external/offers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OfferExternalController {

    private final OfferService offerService;
    private final CloudflareImagesService cloudflareImagesService;
    private final PhoneModelsService phoneModelsService;

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
    public Page<PublicPhoneOffer> getOffers(
        @RequestParam(required = false) String brand,
        @RequestParam(required = false) String model,
        @RequestParam(required = false) UUID phoneModelTechnicalId,
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String location,
        @RequestParam(required = false) BigDecimal minPrice,
        @RequestParam(required = false) BigDecimal maxPrice,
        @RequestParam(required = false) String search,
        @PageableDefault Pageable pageable
    ) {
        Specification<Offer> spec = Specification.allOf(
            OfferSpecifications.hasBrand(brand),
            OfferSpecifications.hasModel(model),
            OfferSpecifications.hasPhoneModelTechnicalId(phoneModelTechnicalId),
            OfferSpecifications.hasName(name),
            OfferSpecifications.hasStatus(status),
            OfferSpecifications.hasLocation(location),
            OfferSpecifications.hasPriceBetween(minPrice, maxPrice),
            OfferSpecifications.search(search),
            OfferSpecifications.orderByModelDisplayPriority()
        );

        log.info("Searching for external offers: {}", spec);
        Pageable priorityPageable = PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize()
        );
        return offerService.getPublicOffers(spec, priorityPageable);
    }

    @GetMapping("/filter-options")
    public Map<String, List<PhoneModelFilterOption>> getFilterOptions() {
        return phoneModelsService.getExternalFilterOptionsByBrand();
    }

    @GetMapping("/{technicalId}")
    public PublicPhoneOffer getOffer(@PathVariable UUID technicalId) {
        return offerService.getPublicOffer(technicalId);
    }
}
