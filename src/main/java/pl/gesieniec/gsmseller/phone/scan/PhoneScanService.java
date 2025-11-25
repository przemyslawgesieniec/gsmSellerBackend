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

    public List<PhoneScanDto> getPhoneScanDtos(String name, String source, String price,
                                               List<MultipartFile> photos) {
        return photos.stream()
            .map(this::toBytes)
            .map(photo -> {
                String extractedData = googleCloudVision.detect(photo);
                PhoneScanDto phoneScanDto = phoneDataOcrParser.parseRawOcrData(extractedData);
                phoneScanDto.setName(name);
                phoneScanDto.setSource(source);
                phoneScanDto.setInitPrice(price);
                return phoneScanDto;
            }).toList();
    }

    @SneakyThrows
    private byte[] toBytes(MultipartFile file) {
        return file.getBytes();
    }
}
