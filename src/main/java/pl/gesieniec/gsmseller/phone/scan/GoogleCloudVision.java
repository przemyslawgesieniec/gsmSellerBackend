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

    private final ImageAnnotatorClient client;

    public GoogleCloudVision(
        @Value("${google.vision.credentials}") String credentialsBase64
    ) {
        if (credentialsBase64 == null || credentialsBase64.isBlank()) {
            throw new IllegalStateException("Google Vision credentials not configured");
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(credentialsBase64);

            GoogleCredentials credentials = GoogleCredentials
                .fromStream(new ByteArrayInputStream(decoded))
                .createScoped("https://www.googleapis.com/auth/cloud-platform");

            ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
                .setCredentialsProvider(() -> credentials)
                .build();

            this.client = ImageAnnotatorClient.create(settings);

            log.info("✅ Google Cloud Vision client initialized once");

        } catch (Exception e) {
            throw new IllegalStateException("Failed to init Google Vision client", e);
        }
    }

    public String detect(byte[] photo) {

        ByteString imgBytes = ByteString.copyFrom(photo);
        Image img = Image.newBuilder().setContent(imgBytes).build();

        Feature feature = Feature.newBuilder()
            .setType(Feature.Type.DOCUMENT_TEXT_DETECTION)
            .build();

        AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
            .addFeatures(feature)
            .setImage(img)
            .build();

        AnnotateImageResponse response =
            client.batchAnnotateImages(List.of(request))
                .getResponses(0);

        if (response.hasError()) {
            throw new IllegalStateException(response.getError().getMessage());
        }

        return response.getFullTextAnnotation().getText();
    }
}
