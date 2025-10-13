package pl.gesieniec.gsmseller.phone.scan;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
public class PhoneScanAiService {

    private final WebClient webClient;
    private final String apiUrl;
    private final String apiKey;

    public PhoneScanAiService(WebClient webClient,
                               @Value("${gpt.key}") String apiKey,
                               @Value("${gpt.url}") String apiUrl) {
        this.webClient = webClient;
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
    }

    public PhoneScanDto scrapDataFromImage(ByteArrayResource photo) throws JsonProcessingException {

        MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
        formData.add("prompt", getPrompt());
        formData.add(UUID.randomUUID().toString(), photo);

        String response = webClient.post()
            .uri(apiUrl)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(formData))
            .retrieve()
            .bodyToMono(String.class)
            .block();
        log.info("Received parsed image response : {}", response);

        PhoneScanDto phoneScanDto = new ObjectMapper().readValue(response, PhoneScanDto.class);

        return phoneScanDto;
    }

    private String getPrompt() {
        return
            "Extract information from the photo. Return value as flat JSON. Try to match extracted values to my model having fields: " +
                "String model;\n" +
                "String ram;\n" +
                "String memory;\n" +
                "String color;\n" +
                "String imei1;\n" +
                "String imei2;\n" +
                "String eid;\n" +
                "String barcode;\n" +
                "String sn;";
    }
}
