package pl.gesieniec.gsmseller.offer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import pl.gesieniec.gsmseller.offer.model.PhoneOffer;
import pl.gesieniec.gsmseller.offer.model.specs.CommunicationSpecs;
import pl.gesieniec.gsmseller.offer.model.specs.ScreenSpecs;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class OfferOpenAIParser {

    private final WebClient openAiWebClient;
    private final ObjectMapper objectMapper;
    private final String model;

    public OfferOpenAIParser(
            WebClient openAiWebClient,
            ObjectMapper objectMapper,
            @Value("${openai.model:gpt-4o-mini}") String model
    ) {
        this.openAiWebClient = openAiWebClient;
        this.objectMapper = objectMapper;
        this.model = model;
    }

    public PhoneOffer fetchSpecsFromAi(String phoneName, String phoneModel) {
        log.info("Fetching phone specs from AI for: {} {}", phoneName, phoneModel);
        try {
            String prompt = buildPrompt(phoneName, phoneModel);

            Map<String, Object> request = Map.of(
                    "model", model,
                    "temperature", 0,
                    "messages", List.of(
                            Map.of(
                                    "role", "system",
                                "content", "Jesteś ekspertem technicznym w dziedzinie telefonów komórkowych. Wyodrębnij dane strukturalne i zwróć WYŁĄCZNIE poprawny JSON."                            ),
                            Map.of(
                                    "role", "user",
                                    "content", prompt
                            )
                    )
            );

            String response = openAiWebClient
                    .post()
                    .uri("/chat/completions")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(20))
                    .block();

            String json = extractJsonFromResponse(response);
            return mapToPhoneOffer(json);

        } catch (Exception e) {
            log.error("❌ Failed to fetch phone specs via OpenAI", e);
            throw new IllegalStateException("Failed to fetch phone specs from AI", e);
        }
    }

    private String buildPrompt(String phoneName, String phoneModel) {
        return """
        Wyszukaj specyfikację techniczną telefonu: %s %s.

        Zwróć WYŁĄCZNIE obiekt JSON z DOKŁADNIE tymi polami:
        {
          "model": string,
          "screen": {
            "size": string,
            "resolution": string,
            "type": string
          },
          "memory": string,
          "ram": string,
          "simCardType": string,
          "frontCamerasMpx": number[],
          "backCamerasMpx": number[],
          "batteryCapacity": string,
          "communication": {
            "wifi": string,
            "portType": string,
            "bluetoothVersion": string
          },
          "operatingSystem": string
        }

        ZASADY:
        - "memory" powinno być stringiem np. "256 GB / 512 GB / 1 TB"
        - "ram" powinno być stringiem np. "12 GB / 16 GB"
        - "frontCamerasMpx" oraz "backCamerasMpx" MUSZĄ być tablicami liczb całkowitych.
        - Jeśli wartość jest nieznana, użyj null dla stringów/obiektów lub pustej tablicy dla list.
        - NIE dodawaj żadnych wyjaśnień.
        - Wynik musi być wyłącznie poprawnym JSON-em.
        """.formatted(phoneName, phoneModel);
    }

    private String extractJsonFromResponse(String response) throws Exception {
        JsonNode root = objectMapper.readTree(response);
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");

        if (contentNode.isMissingNode() || contentNode.isNull()) {
            throw new IllegalStateException("OpenAI response missing content field");
        }

        String content = contentNode.asText().trim();
        if (content.startsWith("```")) {
            content = content
                    .replaceAll("^```json", "")
                    .replaceAll("^```", "")
                    .replaceAll("```$", "")
                    .trim();
        }
        return content;
    }

    private PhoneOffer mapToPhoneOffer(String json) throws Exception {
        JsonNode node = objectMapper.readTree(json);

        return PhoneOffer.builder()
                .model(node.path("model").asText())
                .screen(ScreenSpecs.builder()
                        .size(node.path("screen").path("size").asText())
                        .resolution(node.path("screen").path("resolution").asText())
                        .type(node.path("screen").path("type").asText())
                        .build())
                .memory(node.path("memory").asText())
                .ram(node.path("ram").asText())
                .simCardType(node.path("simCardType").asText())
                .frontCamerasMpx(objectMapper.convertValue(node.path("frontCamerasMpx"), List.class))
                .backCamerasMpx(objectMapper.convertValue(node.path("backCamerasMpx"), List.class))
                .batteryCapacity(node.path("batteryCapacity").asText())
                .communication(CommunicationSpecs.builder()
                        .wifi(node.path("communication").path("wifi").asText())
                        .portType(node.path("communication").path("portType").asText())
                        .bluetoothVersion(node.path("communication").path("bluetoothVersion").asText())
                        .build())
                .operatingSystem(node.path("operatingSystem").asText())
                .build();
    }
}
