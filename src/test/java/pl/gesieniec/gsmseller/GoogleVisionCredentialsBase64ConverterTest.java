package pl.gesieniec.gsmseller;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class GoogleVisionCredentialsBase64ConverterTest {

    @Test
    void shouldConvertGoogleVisionCredentialsJsonToBase64() throws IOException {

        String credentialsJson = "TODO";

        assertNotNull(credentialsJson);
        assertTrue(credentialsJson.contains("private_key"));

        String base64 = Base64.getEncoder()
            .encodeToString(credentialsJson.getBytes(StandardCharsets.UTF_8));


        System.out.println("========== GOOGLE VISION BASE64 ==========");
        System.out.println(base64);
        System.out.println("=========================================");

        // sanity check
        byte[] decoded = Base64.getDecoder().decode(base64);
        assertNotNull(decoded);
    }
}
