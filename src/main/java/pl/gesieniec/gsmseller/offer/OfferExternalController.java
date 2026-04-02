package pl.gesieniec.gsmseller.offer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import pl.gesieniec.gsmseller.offer.model.PhoneOffer;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/external/offers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OfferExternalController {

    private final OfferService offerService;

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
            OfferSpecifications.hasModel(model),
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
