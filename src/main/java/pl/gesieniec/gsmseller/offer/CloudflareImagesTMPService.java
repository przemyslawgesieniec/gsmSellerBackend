package pl.gesieniec.gsmseller.offer;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import pl.gesieniec.gsmseller.configuration.CloudflareProperties;

import java.util.UUID;

@Service
public class CloudflareImagesTMPService {

    private final CloudflareProperties cloudflareProperties;
    private final WebClient webClient;
    private final OfferPhotoRepository offerPhotoRepository;

    public CloudflareImagesTMPService(WebClient.Builder webClientBuilder, CloudflareProperties cloudflareProperties, OfferPhotoRepository offerPhotoRepository) {
        this.cloudflareProperties = cloudflareProperties;
        this.offerPhotoRepository = offerPhotoRepository;
        this.webClient = webClientBuilder
            .baseUrl("https://api.cloudflare.com/client/v4/accounts/" + cloudflareProperties.getAccountId() + "/images/v1")
            .defaultHeader("Authorization", "Bearer " + cloudflareProperties.getApiKey())
            .build();
    }

    @Transactional
    public void processAndUpload(Long photoId) {
        offerPhotoRepository.findById(photoId).ifPresent(photo -> {
            String imageId = uploadImage(photo.getData(), photo.getTechnicalId().toString());
            if (imageId != null) {
                photo.setImageId(imageId);
                offerPhotoRepository.save(photo);
            }
        });
    }

    private String uploadImage(byte[] data, String filename) {
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("file", new ByteArrayResource(data) {
            @Override
            public String getFilename() {
                return filename;
            }
        });
        bodyBuilder.part("requireSignedURLs", "false");

        Map<String, String> metadata = Map.of(
            "env", cloudflareProperties.getEnv(),
            "app", "Teleakcesoria"
        );
        bodyBuilder.part("metadata", metadata);

        Map response = webClient.post()
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
            .retrieve()
            .bodyToMono(Map.class)
            .block();

        Map result = (Map) response.get("result");
        return (String) result.get("id");
    }
}
