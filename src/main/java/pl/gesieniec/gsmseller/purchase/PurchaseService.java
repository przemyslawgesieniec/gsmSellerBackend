package pl.gesieniec.gsmseller.purchase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pl.gesieniec.gsmseller.offer.CloudflareImagesService;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final PurchasePhotoRepository purchasePhotoRepository;
    private final CloudflareImagesService cloudflareImagesService;

    private static final int MAX_PHOTOS = 5;
    private static final long MAX_PHOTO_SIZE = 5 * 1024 * 1024; // 5MB
    private static final Locale POLISH_LOCALE = Locale.forLanguageTag("pl-PL");

    @Transactional(readOnly = true)
    public Page<Purchase> getPurchases(String statusGroup, String search, Pageable pageable) {
        Specification<Purchase> spec = Specification.allOf(
                statusGroupSpecification(statusGroup),
                searchSpecification(search)
        );

        Page<Purchase> purchases = purchaseRepository.findAll(spec, pageable);
        purchases.getContent().forEach(purchase -> purchase.getComments().size());
        return purchases;
    }

    @Transactional(readOnly = true)
    public List<UUID> getPhotoTechnicalIds(UUID purchaseTechnicalId) {
        return purchasePhotoRepository.findTechnicalIdsByPurchaseTechnicalId(purchaseTechnicalId);
    }

    @Transactional
    public void closePurchase(UUID technicalId, String reason, boolean contactedCustomer, String authorUsername) {
        Purchase purchase = purchaseRepository.findByTechnicalId(technicalId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase not found: " + technicalId));
        purchase.close(reason, contactedCustomer);
        purchase.addComment(authorUsername, "Zamknięto skup bez powodzenia. Powód: " + reason.trim()
                + ". Klient poinformowany: " + (contactedCustomer ? "tak" : "nie") + ".");
        purchaseRepository.save(purchase);
        log.info("Closed purchase request: {} with reason: {}", technicalId, reason);
    }

    @Transactional
    public void agreePrice(UUID technicalId, BigDecimal agreedPrice, String authorUsername) {
        Purchase purchase = purchaseRepository.findByTechnicalId(technicalId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase not found: " + technicalId));
        purchase.agreePrice(agreedPrice);
        purchase.addComment(authorUsername, "Ustalono cenę skupu: " + formatPrice(agreedPrice) + ".");
        purchaseRepository.save(purchase);
        log.info("Agreed purchase price for request {}: {}", technicalId, agreedPrice);
    }

    @Transactional
    public void markPurchased(UUID technicalId, BigDecimal agreedPrice, String authorUsername) {
        Purchase purchase = purchaseRepository.findByTechnicalId(technicalId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase not found: " + technicalId));
        purchase.markPurchased(agreedPrice);
        purchase.addComment(authorUsername, "Skup zakończony sukcesem. Kwota: " + formatPrice(agreedPrice) + ".");
        purchaseRepository.save(purchase);
        log.info("Marked purchase request {} as successfully purchased for {}", technicalId, agreedPrice);
    }

    @Transactional
    public void addComment(UUID technicalId, String authorUsername, String content) {
        Purchase purchase = purchaseRepository.findByTechnicalId(technicalId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase not found: " + technicalId));
        purchase.addComment(authorUsername, content);
        purchaseRepository.save(purchase);
        log.info("Added comment to purchase request {} by {}", technicalId, authorUsername);
    }

    @Transactional(readOnly = true)
    public PurchasePhoto getPhoto(UUID photoTechnicalId) {
        PurchasePhoto photo = purchasePhotoRepository.findByTechnicalId(photoTechnicalId)
                .orElseThrow(() -> new IllegalArgumentException("Photo not found: " + photoTechnicalId));
        return photo;
    }

    @Transactional
    public void createPurchase(String phoneModel, String phoneNumber, String description, List<MultipartFile> photos) {
        if (photos != null && photos.size() > MAX_PHOTOS) {
            throw new IllegalArgumentException("Maximum " + MAX_PHOTOS + " photos are allowed");
        }

        List<PurchasePhoto> purchasePhotos = new ArrayList<>();
        if (photos != null) {
            for (MultipartFile photo : photos) {
                if (photo.isEmpty()) continue;
                
                if (photo.getSize() > MAX_PHOTO_SIZE) {
                    throw new IllegalArgumentException("Each photo must be maximum 5MB");
                }

                if (photo.getContentType() == null || !photo.getContentType().startsWith("image/")) {
                    throw new IllegalArgumentException("Only image files are allowed");
                }

                try {
                    String imageId = cloudflareImagesService.uploadImage(photo);
                    purchasePhotos.add(new PurchasePhoto(photo.getContentType(), imageId));
                } catch (IOException e) {
                    log.error("Failed to upload purchase image to Cloudflare", e);
                    throw new RuntimeException("Failed to upload image", e);
                }
            }
        }

        Purchase purchase = new Purchase(phoneModel, phoneNumber, description, purchasePhotos);
        purchaseRepository.save(purchase);
        log.info("Saved new purchase request for model: {} from number: {}", phoneModel, phoneNumber);
    }

    private Specification<Purchase> statusGroupSpecification(String statusGroup) {
        return (root, query, cb) -> {
            if (statusGroup == null || statusGroup.isBlank() || "all".equalsIgnoreCase(statusGroup)) {
                return null;
            }

            if ("active".equalsIgnoreCase(statusGroup)) {
                return root.get("status").in(List.of(PurchaseStatus.NEW, PurchaseStatus.OPEN, PurchaseStatus.PRICE_AGREED));
            }

            if ("closed".equalsIgnoreCase(statusGroup)) {
                return root.get("status").in(List.of(PurchaseStatus.CLOSED, PurchaseStatus.PURCHASED));
            }

            throw new IllegalArgumentException("Unsupported status group: " + statusGroup);
        };
    }

    private Specification<Purchase> searchSpecification(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) {
                return null;
            }

            String pattern = "%" + search.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("phoneModel")), pattern),
                    cb.like(cb.lower(root.get("phoneNumber")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
            );
        };
    }

    private String formatPrice(BigDecimal price) {
        return NumberFormat.getCurrencyInstance(POLISH_LOCALE).format(price);
    }
}
