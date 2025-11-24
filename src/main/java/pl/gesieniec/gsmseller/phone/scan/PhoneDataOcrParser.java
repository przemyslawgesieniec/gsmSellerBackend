package pl.gesieniec.gsmseller.phone.scan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class PhoneDataOcrParser {

    public PhoneScanDto parseRawOcrData(String raw) {

        Map<String, String> result = new HashMap<>();

        findImei(raw, result);
        findRamAndStorage(raw, result);
        findColor(raw, result);
        findModel(raw, result);

        return new PhoneScanDto(
            result.get("model"),
            result.get("ram"),
            result.get("storage"),
            result.get("color"),
            result.get("imei"));
    }

    private void findModel(String raw, Map<String, String> result) {
        String model = ModelExtractor.extractModel(raw);
        result.put("model", model);

    }

    private void findColor(String raw, Map<String, String> result) {
        String color = ColorExtractor.extractColor(raw);
        result.put("color", color);
    }

    private void findRamAndStorage(String raw, Map<String, String> result) {
        Pattern memoryPattern = Pattern.compile("(\\d{1,4})\\s*GB", Pattern.CASE_INSENSITIVE);
        Matcher memoryMatcher = memoryPattern.matcher(raw);

        List<Integer> memoryValues = new ArrayList<>();

        while (memoryMatcher.find()) {
            int value = Integer.parseInt(memoryMatcher.group(1));
            memoryValues.add(value);
        }

        if (memoryValues.size() >= 1) {
            int min = Collections.min(memoryValues);
            int max = Collections.max(memoryValues);

            result.put("ram", min + "GB");
            result.put("storage", max + "GB");
        }
    }

    private void findImei(String raw, Map<String, String> result) {
        Pattern imeiPattern = Pattern.compile("\\b\\d{15}\\b");
        Matcher imeiMatcher = imeiPattern.matcher(raw);
        if (imeiMatcher.find()) {
            result.put("imei", imeiMatcher.group());
        }
    }


}
