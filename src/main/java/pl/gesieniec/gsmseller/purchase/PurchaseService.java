package pl.gesieniec.gsmseller.purchase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pl.gesieniec.gsmseller.storage.FileStorageService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final PurchasePhotoRepository purchasePhotoRepository;
    private final FileStorageService fileStorageService;

    private static final int MAX_PHOTOS = 5;
    private static final long MAX_PHOTO_SIZE = 5 * 1024 * 1024; // 5MB

    @Transactional(readOnly = true)
    public List<Purchase> getAllPurchases() {
        return purchaseRepository.findAllWithPhotos();
    }

    @Transactional
    public void closePurchase(UUID technicalId, String reason, boolean contactedCustomer) {
        Purchase purchase = purchaseRepository.findByTechnicalId(technicalId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase not found: " + technicalId));
        purchase.close(reason, contactedCustomer);
        purchaseRepository.save(purchase);
        log.info("Closed purchase request: {} with reason: {}", technicalId, reason);
    }

    @Transactional(readOnly = true)
    public PurchasePhoto getPhoto(UUID photoTechnicalId) {
        PurchasePhoto photo = purchasePhotoRepository.findByTechnicalId(photoTechnicalId)
                .orElseThrow(() -> new IllegalArgumentException("Photo not found: " + photoTechnicalId));
        if (photo.getData() != null) {
            int length = photo.getData().length; // Force load @Lob within transaction
        }
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

                byte[] compressedData = fileStorageService.compressImageIfImage(photo);
                purchasePhotos.add(new PurchasePhoto(compressedData, photo.getContentType()));
            }
        }

        Purchase purchase = new Purchase(phoneModel, phoneNumber, description, purchasePhotos);
        purchaseRepository.save(purchase);
        log.info("Saved new purchase request for model: {} from number: {}", phoneModel, phoneNumber);
    }
}
