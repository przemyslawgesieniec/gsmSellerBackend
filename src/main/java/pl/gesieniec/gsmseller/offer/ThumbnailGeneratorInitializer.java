package pl.gesieniec.gsmseller.offer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pl.gesieniec.gsmseller.storage.FileStorageService;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ThumbnailGeneratorInitializer implements CommandLineRunner {

    private final OfferPhotoRepository offerPhotoRepository;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Checking for offer photos without thumbnails...");
        List<OfferPhoto> photosWithoutThumbnail = offerPhotoRepository.findAllByThumbnailDataIsNull();
        
        if (photosWithoutThumbnail.isEmpty()) {
            log.info("No photos without thumbnails found.");
            return;
        }

        log.info("Found {} photos without thumbnails. Generating...", photosWithoutThumbnail.size());
        
        int count = 0;
        for (OfferPhoto photo : photosWithoutThumbnail) {
            try {
                byte[] thumbnail = fileStorageService.createThumbnail(photo.getData(), photo.getContentType());
                if (thumbnail != null) {
                    photo.setThumbnailData(thumbnail);
                    offerPhotoRepository.save(photo);
                    count++;
                }
            } catch (Exception e) {
                log.error("Failed to generate thumbnail for photo ID: {}", photo.getId(), e);
            }
        }
        
        log.info("Successfully generated {} thumbnails.", count);
    }
}
