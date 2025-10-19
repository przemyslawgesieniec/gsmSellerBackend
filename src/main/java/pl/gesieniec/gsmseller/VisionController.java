package pl.gesieniec.gsmseller;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/gpt/test")
public class VisionController {

    private final String apiKey;
    private final String apiBase;
    private final String model;

    public VisionController(@Value("${openai.api-key}") String apiKey,
                            @Value("${openai.api-base}") String apiBase,
                            @Value("${openai.model}") String model) {
        this.apiKey = apiKey;
        this.apiBase = apiBase;
        this.model = model;

    }

    @SneakyThrows
    @PostMapping(value = "/analyze-image")
    public ResponseEntity<Map<String, Object>> analyzeImage() {

        String prompt = """
            Extract all possible data from the picture. 
            Return it as a list of JSON objects. 
            Each object containing keys: name, model, memory, ram, imei1, imei2, color. 
            Not matched values leave as null.
        """;

        String imagePath = "static/IMG_1603A6853523-1.jpeg";
        String dataUrl = imageToDataUrl(imagePath);

        // Utwórz JSON payload ręcznie
        Map<String, Object> textObj = Map.of("type", "input_text", "text", prompt);
        Map<String, Object> imageObj = Map.of(
            "type", "input_image",
            "image_url", Map.of("url", dataUrl)
        );
        Map<String, Object> userMessage = Map.of(
            "role", "user",
            "content", List.of(textObj, imageObj)
        );
        Map<String, Object> body = Map.of(
            "model", "gpt-4o-mini",
            "input", List.of(userMessage)
        );

        // Zamiana Map na JSON
        String jsonBody = new ObjectMapper().writeValueAsString(body);

        System.out.println("JSON body: ");
        System.out.println(jsonBody);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(apiBase + "/responses"))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
            .build();

        System.out.println("⏳ Sending request to OpenAI...");

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("\n✅ OpenAI Response:");
        System.out.println(response.body());

        Map<String, Object> result = new HashMap<>();
        result.put("openai_response", response.body());
        return ResponseEntity.ok(result);
    }

    private String imageToDataUrl(String resourcePath) throws IOException {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        try (InputStream inputStream = resource.getInputStream()) {
            byte[] bytes = inputStream.readAllBytes();
            String base64 = Base64.encodeBase64String(bytes);
            return "data:image/jpeg;base64," + base64;
        }
    }

}
