package pl.gesieniec.gsmseller.phone.scan;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


@Slf4j
@Service
@AllArgsConstructor
public class PhoneScanService {

    private final GoogleCloudVision googleCloudVision;
    private final PhoneDataOcrParser phoneDataOcrParser;

    public List<PhoneScanDto> getPhoneScanDtos(
        String name,
        String source,
        String initialPrice,
        String sellingPrice,
        List<MultipartFile> photos
    ) {

        log.info("📦 Starting phone scan process");
        log.info("Metadata: name={}, source={}, initialPrice={}, sellingPrice={}",
            name, source, initialPrice, sellingPrice);

        log.info("Photos count: {}", photos != null ? photos.size() : 0);

        return photos.stream()
            .map(photo -> {
                log.info("➡️ Processing photo: {}", photo.getOriginalFilename());

                byte[] bytes = toBytes(photo);
                log.debug("Photo '{}' byte size: {}", photo.getOriginalFilename(), bytes.length);

                String extractedData;
                try {
                    extractedData = googleCloudVision.detect(bytes);
                } catch (Exception e) {
                    log.error("❌ OCR failed for photo: {}", photo.getOriginalFilename(), e);
                    throw e;
                }

                log.info("➡️ Parsing OCR data for photo: {}", photo.getOriginalFilename());

                PhoneScanDto phoneScanDto;
                try {
                    phoneScanDto = phoneDataOcrParser.parseRawOcrData(extractedData);
                } catch (Exception e) {
                    log.error("❌ OCR parsing failed for photo: {}", photo.getOriginalFilename(), e);
                    throw e;
                }

                phoneScanDto.setName(name);
                phoneScanDto.setSource(source);
                phoneScanDto.setInitialPrice(initialPrice);
                phoneScanDto.setSellingPrice(sellingPrice);

                log.info("✅ PhoneScanDto created for photo: {}", photo.getOriginalFilename());

                return phoneScanDto;
            })
            .toList();
    }

    @SneakyThrows
    private byte[] toBytes(MultipartFile file) {
        log.debug("Reading bytes from MultipartFile: {}", file.getOriginalFilename());
        return file.getBytes();
    }
}
