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
import pl.gesieniec.gsmseller.offer.model.PhoneOffer;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/external/offers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OfferExternalController {

    private final OfferService offerService;

    @GetMapping("/photos")
    public ResponseEntity<List<byte[]>> getPhotos(@RequestParam List<UUID> ids) {
        List<OfferPhoto> photos = offerService.getPhotos(ids);
        List<byte[]> data = photos.stream().map(OfferPhoto::getData).toList();
        return ResponseEntity.ok(data);
    }

    @GetMapping("/photos/{id}")
    public ResponseEntity<byte[]> getPhoto(@PathVariable UUID id) {
        OfferPhoto photo = offerService.getPhotos(List.of(id)).stream().findFirst()
            .orElseThrow(() -> new pl.gesieniec.gsmseller.common.EntityNotFoundException("Photo not found: " + id));
        
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(photo.getContentType()))
            .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000")
            .body(photo.getData());
    }

    @GetMapping("/photos/{id}/thumbnail")
    public ResponseEntity<byte[]> getPhotoThumbnail(@PathVariable UUID id) {
        OfferPhoto photo = offerService.getPhotos(List.of(id)).stream().findFirst()
            .orElseThrow(() -> new pl.gesieniec.gsmseller.common.EntityNotFoundException("Photo not found: " + id));

        byte[] thumbnailData = photo.getThumbnailData();
        if (thumbnailData == null || thumbnailData.length == 0) {
            // Fallback do pełnego zdjęcia jeśli miniatura nie istnieje
            thumbnailData = photo.getData();
        }

        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_JPEG)
            .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000")
            .body(thumbnailData);
    }

    @GetMapping
    public Page<PhoneOffer> getOffers(
        @RequestParam(required = false) String brand,
        @RequestParam(required = false) String model,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String location,
        @RequestParam(required = false) BigDecimal minPrice,
        @RequestParam(required = false) BigDecimal maxPrice,
        @RequestParam(required = false) String search,
        @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Specification<Offer> spec = Specification.allOf(
            OfferSpecifications.hasBrand(brand),
            OfferSpecifications.hasStatus(status),
            OfferSpecifications.hasLocation(location),
            OfferSpecifications.hasPriceBetween(minPrice, maxPrice),
            OfferSpecifications.search(search)
        );

        log.info("Searching for external offers: {}", spec);
        return offerService.getOffers(spec, pageable);
    }

    @GetMapping("/{technicalId}")
    public PhoneOffer getOffer(@PathVariable UUID technicalId) {
        return offerService.getOffer(technicalId);
    }
}
