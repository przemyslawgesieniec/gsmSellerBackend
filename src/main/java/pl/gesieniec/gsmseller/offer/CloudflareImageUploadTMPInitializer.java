package pl.gesieniec.gsmseller.offer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CloudflareImageUploadTMPInitializer {

    private final OfferPhotoRepository offerPhotoRepository;
    private final CloudflareImagesTMPService cloudflareImagesService;

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void uploadMissingImages() {
        log.info("Starting background process for uploading missing offer photos to Cloudflare...");
        
        List<Long> photoIdsToUpload;
        while (!(photoIdsToUpload = offerPhotoRepository.findIdsByImageIdIsNull(PageRequest.of(0, 10))).isEmpty()) {
            log.info("Found {} photos without Cloudflare imageId. Uploading...", photoIdsToUpload.size());
            
            for (Long photoId : photoIdsToUpload) {
                try {
                    cloudflareImagesService.processAndUpload(photoId);
                } catch (Exception e) {
                    log.error("Failed to upload photo ID: {} to Cloudflare", photoId, e);
                    // Odczekujemy chwilę przed kolejną próbą jeśli wystąpił błąd komunikacji
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
            log.info("Finished processing batch of {} photos.", photoIdsToUpload.size());
            
            // Krótka przerwa między batchami (jeśli baza została zaktualizowana w międzyczasie)
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        log.info("No more photos to upload to Cloudflare. Task finished.");
    }
}
