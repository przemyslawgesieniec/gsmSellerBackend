package pl.gesieniec.gsmseller.phone.scan.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import pl.gesieniec.gsmseller.phone.scan.PhoneScanDto;

@Slf4j
@Component
@Primary
public class OpenAiPhoneScanOcrParser implements OcrDataParser {

    private final WebClient openAiWebClient;
    private final ObjectMapper objectMapper;

    public OpenAiPhoneScanOcrParser(
        WebClient openAiWebClient,
        ObjectMapper objectMapper
    ) {
        this.openAiWebClient = openAiWebClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public PhoneScanDto parseRawOcrData(String raw) {

        try {
            String prompt = buildPrompt(raw);

            Map<String, Object> request = Map.of(
                "model", "gpt-4o-mini",
                "temperature", 0,
                "messages", List.of(
                    Map.of(
                        "role", "system",
                        "content", "You extract structured data and return ONLY valid JSON."
                    ),
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
                .timeout(Duration.ofSeconds(15))
                .block();

            String json = extractJsonFromResponse(response);

            PhoneScanDto dto = objectMapper.readValue(json, PhoneScanDto.class);
            dto.normalizeData();

            return dto;

        } catch (Exception e) {
            log.error("❌ Failed to parse OCR data via OpenAI", e);
            throw new IllegalStateException("Failed to parse OCR phone data", e);
        }
    }

    private String buildPrompt(String raw) {
        return """
            From the OCR text below, extract phone information.

            OCR TEXT:
            ---
            %s
            ---

            Return ONLY a JSON object with EXACTLY these fields:
            {
              "model": string | null,
              "ram": string | null,
              "memory": string | null,
              "color": string | null,
              "imei": string | null
            }

            RULES:
            - Do NOT guess values
            - If a value is missing or uncertain, use null
            - Do NOT add extra fields
            - Do NOT add explanations
            - Output must be valid JSON only
            """.formatted(raw);
    }

    /**
     * Bardzo defensywne wyciągnięcie JSON-a z odpowiedzi OpenAI
     */
    private String extractJsonFromResponse(String response) throws Exception {

        JsonNode root = objectMapper.readTree(response);

        JsonNode contentNode = root
            .path("choices")
            .path(0)
            .path("message")
            .path("content");

        if (contentNode.isMissingNode() || contentNode.isNull()) {
            throw new IllegalStateException("OpenAI response missing content field");
        }

        String content = contentNode.asText().trim();

        // zabezpieczenie: czasem model owija JSON w ```json
        if (content.startsWith("```")) {
            content = content
                .replaceAll("^```json", "")
                .replaceAll("^```", "")
                .replaceAll("```$", "")
                .trim();
        }

        return content;
    }
}
