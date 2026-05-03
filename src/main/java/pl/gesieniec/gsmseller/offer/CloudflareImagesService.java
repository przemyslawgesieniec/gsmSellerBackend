package pl.gesieniec.gsmseller.offer;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import pl.gesieniec.gsmseller.configuration.CloudflareProperties;

@Service
public class CloudflareImagesService {

    private final CloudflareProperties cloudflareProperties;
    private final WebClient webClient;

    public CloudflareImagesService(WebClient.Builder webClientBuilder, CloudflareProperties cloudflareProperties) {
        this.cloudflareProperties = cloudflareProperties;
        this.webClient = webClientBuilder
            .baseUrl("https://api.cloudflare.com/client/v4/accounts/" + cloudflareProperties.getAccountId() + "/images/v1")
            .defaultHeader("Authorization", "Bearer " + cloudflareProperties.getApiKey())
            .build();
    }

    public String uploadImage(MultipartFile file, String offerId) throws IOException {
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("file", file.getResource());
        bodyBuilder.part("metadata", "{\"offerId\":\"" + offerId + "\"}");
        bodyBuilder.part("requireSignedURLs", "false");

        // Opcjonalnie: custom ID powiązane z ofertą
        // bodyBuilder.part("id", "offers/" + offerId + "/" + file.getOriginalFilename());

        Map response = webClient.post()
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
            .retrieve()
            .bodyToMono(Map.class)
            .block();

        Map result = (Map) response.get("result");
        String imageId = (String) result.get("id");
        List<String> variants = (List<String>) result.get("variants");

        // Zapisz imageId do bazy danych oferty
        return imageId;
    }

    public void deleteImage(String imageId) {
        webClient.delete()
            .uri("/{imageId}", imageId)
            .retrieve()
            .bodyToMono(Void.class)
            .block();
    }

    public String getImageUrl(String imageId, String variant) {
        return "https://imagedelivery.net/" + cloudflareProperties.getAccountHash() + "/" + imageId + "/" + variant;
    }
}
