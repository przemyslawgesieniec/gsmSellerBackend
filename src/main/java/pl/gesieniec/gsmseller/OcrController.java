package pl.gesieniec.gsmseller;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@RestController
public class OcrController {

    @Value("classpath:static/IMG_1603A6853523-1.jpeg")
    Resource resourceFile;

    @GetMapping("/api/ocr")
    public Map<String, Object> performOcr() {
        Map<String, Object> result = new HashMap<>();

        try {
            System.setProperty("jna.library.path", "//opt/homebrew/Cellar/tesseract/5.5.1/lib");

            // pobierz obraz z classpath (src/main/resources/static/test.jpg)
           File file = resourceFile.getFile();

            ITesseract tesseract = new Tesseract();
            tesseract.setLanguage("eng");
            tesseract.setDatapath("/opt/homebrew/Cellar/tesseract/5.5.1/share"); // folder zawierający tessdata

            String text = tesseract.doOCR(file);

            result.put("status", "success");
            result.put("text", text);
        } catch (TesseractException e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "Nie znaleziono pliku test.jpg");
            e.printStackTrace();
        }

        return result;
    }
}
