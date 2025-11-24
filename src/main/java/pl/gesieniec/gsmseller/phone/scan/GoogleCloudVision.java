package pl.gesieniec.gsmseller.phone.scan;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageAnnotatorSettings;
import com.google.protobuf.ByteString;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import lombok.SneakyThrows;

public class GoogleCloudVision {

    @SneakyThrows
    public String detect() {

        GoogleCredentials credentials = GoogleCredentials
            .fromStream(new FileInputStream(
                "/Users/przemyslawgesieniec/privateDev/kluczeGoogle/gsm-seller-478711-f061eaeea1b8.json"))
            .createScoped("https://www.googleapis.com/auth/cloud-platform");

        ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
            .setCredentialsProvider(() -> credentials)
            .build();

        byte[] data = Files.readAllBytes(Paths.get("src/main/resources/static/IMG_1603A6853523-1.jpeg"));
        ByteString imgBytes = ByteString.copyFrom(data);

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
                System.err.println("Error: " + response.getError().getMessage());
                return "";
            }

            // 4. Cały tekst
            String fullText = response.getFullTextAnnotation().getText();
            System.out.println("=== Odczytany tekst ===\n" + fullText);

            return fullText;

        }
    }
}
