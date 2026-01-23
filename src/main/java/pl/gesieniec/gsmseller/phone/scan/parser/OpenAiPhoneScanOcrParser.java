package pl.gesieniec.gsmseller.phone.scan.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import pl.gesieniec.gsmseller.phone.scan.PhoneScanDto;

@Slf4j
@Component
@Primary
public class OpenAiPhoneScanOcrParser implements OcrDataParser {

    private final WebClient openAiWebClient;
    private final ObjectMapper objectMapper;

    public OpenAiPhoneScanOcrParser(WebClient openAiWebClient,
                                    ObjectMapper objectMapper) {
        this.openAiWebClient = openAiWebClient;
        this.objectMapper = objectMapper;
    }

    public PhoneScanDto parseRawOcrData(String raw) {

        log.info("Using OpenAiPhoneScanOcrParser to parse {}", raw);
        try {
            String prompt = buildPrompt(raw);

            Map<String, Object> request = Map.of(
                    "model", "gpt-4o-mini",
                    "temperature", 0,
                    "messages", List.of(
                            Map.of(
                                    "role", "system",
                                    "content", "You are a data extraction engine. You must return ONLY valid JSON."
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
                    .block();

            String json = extractJson(response);

            PhoneScanDto phoneScanDto = objectMapper.readValue(json, PhoneScanDto.class);
            phoneScanDto.normalizeData();

            return phoneScanDto;

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse OCR phone data", e);
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
     * Bezpieczne wyciągnięcie JSONa z odpowiedzi modelu
     */
    private String extractJson(String response) throws Exception {
        var tree = objectMapper.readTree(response);
        return tree
                .path("choices")
                .get(0)
                .path("message")
                .path("content")
                .asText()
                .trim();
    }
}
