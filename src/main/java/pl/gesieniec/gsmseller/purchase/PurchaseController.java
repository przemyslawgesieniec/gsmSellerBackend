package pl.gesieniec.gsmseller.purchase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.gesieniec.gsmseller.offer.CloudflareImagesService;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/purchase")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;
    private final CloudflareImagesService cloudflareImagesService;

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleIllegalArgumentException(IllegalArgumentException e) {
        return e.getMessage();
    }

    @GetMapping
    public Page<PurchaseResponse> getPurchases(
            @RequestParam(required = false, defaultValue = "all") String statusGroup,
            @RequestParam(required = false) String search,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return purchaseService.getPurchases(statusGroup, search, pageable)
                .map(this::mapToResponse);
    }

    @PostMapping("/{technicalId}/close")
    public void closePurchase(
            @PathVariable UUID technicalId,
            @RequestBody ClosePurchaseRequest request,
            Principal principal
    ) {
        log.info("Closing purchase: {} with reason: {} and contacted: {}", technicalId, request.reason(), request.contactedCustomer());
        purchaseService.closePurchase(technicalId, request.reason(), request.contactedCustomer(), getAuthor(principal));
    }

    @PostMapping("/{technicalId}/price-agreed")
    public void agreePrice(
            @PathVariable UUID technicalId,
            @RequestBody PriceRequest request,
            Principal principal
    ) {
        purchaseService.agreePrice(technicalId, request.agreedPrice(), getAuthor(principal));
    }

    @PostMapping("/{technicalId}/success")
    public void markPurchased(
            @PathVariable UUID technicalId,
            @RequestBody PriceRequest request,
            Principal principal
    ) {
        purchaseService.markPurchased(technicalId, request.agreedPrice(), getAuthor(principal));
    }

    @PostMapping("/{technicalId}/comments")
    public void addComment(
            @PathVariable UUID technicalId,
            @RequestBody CommentRequest request,
            Principal principal
    ) {
        purchaseService.addComment(technicalId, getAuthor(principal), request.content());
    }

    @GetMapping("/photos/{photoTechnicalId}")
    public ResponseEntity<?> getPhoto(@PathVariable UUID photoTechnicalId) {
        PurchasePhoto photo = purchaseService.getPhoto(photoTechnicalId);
        return buildPhotoResponse(photo, "public");
    }

    @GetMapping("/photos/{photoTechnicalId}/thumbnail")
    public ResponseEntity<?> getPhotoThumbnail(@PathVariable UUID photoTechnicalId) {
        PurchasePhoto photo = purchaseService.getPhoto(photoTechnicalId);
        return buildPhotoResponse(photo, "thumbnail");
    }

    private ResponseEntity<?> buildPhotoResponse(PurchasePhoto photo, String variant) {
        if (photo.getImageId() != null) {
            String imageUrl = cloudflareImagesService.getImageUrl(photo.getImageId(), variant);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, imageUrl)
                    .build();
        }

        if (photo.getData() == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.getContentType()))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000")
                .body(photo.getData());
    }

    private PurchaseResponse mapToResponse(Purchase purchase) {
        return new PurchaseResponse(
                purchase.getTechnicalId(),
                purchase.getPhoneModel(),
                purchase.getPhoneNumber(),
                purchase.getDescription(),
                purchase.getStatus(),
                purchase.getCreatedAt(),
                purchaseService.getPhotoTechnicalIds(purchase.getTechnicalId()),
                purchase.getAgreedPrice(),
                purchase.getClosureReason(),
                purchase.getContactedCustomer(),
                purchase.getComments().stream().map(this::mapToCommentResponse).toList()
        );
    }

    private PurchaseCommentResponse mapToCommentResponse(PurchaseComment comment) {
        return new PurchaseCommentResponse(
                comment.getTechnicalId(),
                comment.getAuthorUsername(),
                comment.getCreatedAt(),
                comment.getContent()
        );
    }

    private String getAuthor(Principal principal) {
        return principal != null ? principal.getName() : "unknown";
    }

    public record ClosePurchaseRequest(String reason, boolean contactedCustomer) {}

    public record PriceRequest(BigDecimal agreedPrice) {}

    public record CommentRequest(String content) {}

    public record PurchaseResponse(
            UUID technicalId,
            String phoneModel,
            String phoneNumber,
            String description,
            PurchaseStatus status,
            LocalDateTime createdAt,
            List<UUID> photoIds,
            BigDecimal agreedPrice,
            String closureReason,
            Boolean contactedCustomer,
            List<PurchaseCommentResponse> comments
    ) {}

    public record PurchaseCommentResponse(
            UUID technicalId,
            String authorUsername,
            LocalDateTime createdAt,
            String content
    ) {}
}
