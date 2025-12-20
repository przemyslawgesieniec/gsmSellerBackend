package pl.gesieniec.gsmseller.phone.scan;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GoogleCloudVision {

    @Value("${google.vision.credentials}")
    private String credentialsBase64;

    @SneakyThrows
    public String detect(byte[] photo) {

        log.info("📸 Starting Google Cloud Vision OCR (Base64 credentials)");

        if (credentialsBase64 == null || credentialsBase64.isBlank()) {
            log.error("❌ Google Vision Base64 credentials are EMPTY");
            throw new IllegalStateException("Google Vision credentials not configured");
        }

        byte[] decodedCredentials;
        try {
            decodedCredentials = Base64.getDecoder().decode(credentialsBase64);
            log.info("✅ Base64 credentials decoded, size: {}", decodedCredentials.length);
        } catch (Exception e) {
            log.error("❌ Failed to decode Base64 credentials", e);
            throw e;
        }

        GoogleCredentials credentials;
        try {
            credentials = GoogleCredentials
                .fromStream(new ByteArrayInputStream(decodedCredentials))
                .createScoped("https://www.googleapis.com/auth/cloud-platform");

            log.info("✅ Google credentials created");

        } catch (Exception e) {
            log.error("❌ Failed to create GoogleCredentials", e);
            throw e;
        }

        ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
            .setCredentialsProvider(() -> credentials)
            .build();

        ByteString imgBytes = ByteString.copyFrom(photo);
        Image img = Image.newBuilder().setContent(imgBytes).build();

        Feature feature = Feature.newBuilder()
            .setType(Feature.Type.DOCUMENT_TEXT_DETECTION)
            .build();

        AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
            .addFeatures(feature)
            .setImage(img)
            .build();

        log.info("➡️ Sending OCR request to Google Cloud Vision API");

        try (ImageAnnotatorClient client = ImageAnnotatorClient.create(settings)) {

            AnnotateImageResponse response =
                client.batchAnnotateImages(List.of(request))
                    .getResponses(0);

            if (response.hasError()) {
                log.error("❌ Google Vision API error: {}", response.getError().getMessage());
                throw new IllegalStateException(response.getError().getMessage());
            }

            String text = response.getFullTextAnnotation().getText();
            log.info("✅ OCR completed, extracted text length: {}", text != null ? text.length() : 0);

            return text;
        }
    }
}
