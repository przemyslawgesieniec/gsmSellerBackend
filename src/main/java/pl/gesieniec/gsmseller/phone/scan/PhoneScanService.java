package pl.gesieniec.gsmseller.phone.scan;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@AllArgsConstructor
public class PhoneScanService {

    private final GoogleCloudVision googleCloudVision;
    private final PhoneDataOcrParser phoneDataOcrParser;

    public List<PhoneScanDto> getPhoneScanDtos(List<MultipartFile> photos) {
        return photos.stream()
            .map(this::toBytes)
            .map(photo -> {
                String extractedData = googleCloudVision.detect(photo);
                return phoneDataOcrParser.parseRawOcrData(extractedData);
            }).toList();
    }

    @SneakyThrows
    private byte[] toBytes(MultipartFile file) {
        return file.getBytes();
    }
}
