package pl.gesieniec.gsmseller.phone.scan;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GoogleCloudVision {

    @Value("${google.vision.credentials}")
    private String credentialsJson;

    @SneakyThrows
    public String detect(byte[] photo) {

        GoogleCredentials credentials = GoogleCredentials
            .fromStream(
                new ByteArrayInputStream(
                    credentialsJson.getBytes(StandardCharsets.UTF_8)
                )
            )
            .createScoped("https://www.googleapis.com/auth/cloud-platform");

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

        try (ImageAnnotatorClient client = ImageAnnotatorClient.create(settings)) {

            BatchAnnotateImagesResponse batchResponse =
                client.batchAnnotateImages(List.of(request));

            AnnotateImageResponse response = batchResponse.getResponses(0);

            if (response.hasError()) {
                throw new IllegalStateException(response.getError().getMessage());
            }

            return response
                .getFullTextAnnotation()
                .getText();
        }
    }
}
