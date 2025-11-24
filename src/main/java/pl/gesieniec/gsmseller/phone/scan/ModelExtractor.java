package pl.gesieniec.gsmseller.phone.scan;

import java.util.*;
import java.util.regex.*;

public class ModelExtractor {

    private static final List<String> MODEL_PREFIXES = List.of(
        // Samsung
        "SM-", "GT-", "SCH-", "SGH-", "SHV-", "SMG-",

        // Apple
        "iPhone", "A",

        // Xiaomi / Redmi / POCO
        "Xiaomi", "Mi", "MIX", "Redmi", "POCO", "M",

        // Oppo
        "Oppo", "CPH", "CPH2", "CPH3", "PD", "PG",

        // Vivo
        "Vivo", "V", "Y", "T", "iQOO",

        // Realme
        "Realme", "RMX", "RMX3", "RMX4",

        // Motorola
        "Moto", "XT", "XT2", "XT3",

        // Huawei
        "Huawei", "HMA", "LYA", "ANE", "MAR", "STK", "P", "Mate", "Nova",

        // Honor
        "Honor", "HN", "LRA",

        // OnePlus
        "OnePlus", "IN", "NE",

        // Google Pixel
        "Pixel", "G", "GE", "GX",

        // Sony Xperia
        "Xperia", "SO-", "SOG", "H", "G", "F",

        // Nokia
        "Nokia", "TA-", "C", "X",

        // Asus
        "ASUS", "ZS", "AI", "I00", "ROG",

        // Nothing
        "Nothing",

        // ZTE
        "ZTE", "Blade", "Axon", "N",

        // Lenovo / Motorola subbrands
        "Lenovo", "K", "L",

        // Tecno / Infinix / Itel
        "Tecno", "Infinix", "Itel", "KG", "KC", "SPARK", "HOT"
    );


    public static String extractModel(String text) {

        String[] lines = text.split("\\R");

        // Regex – model w jednej linii
        Pattern modelPattern = Pattern.compile("^(?=.*\\d)[A-Z][A-Za-z0-9/\\- ]{2,30}$");

        // Prefiksy poprawiają scoring
        List<String> prefixes = MODEL_PREFIXES;

        String best = null;
        int bestScore = 0;

        for (String rawLine : lines) {

            String line = rawLine.trim();

            if (line.isEmpty()) continue;

            Matcher m = modelPattern.matcher(line);
            if (m.find()) {

                int score = 0;
                for (String prefix : prefixes) {
                    if (line.startsWith(prefix)) {
                        score += 50;
                        break;
                    }
                }

                // +20 jeśli ma slash (np. /DS)
                if (line.contains("/")) score += 20;

                // +20 jeśli ma myślnik (np. SM-S901B)
                if (line.contains("-")) score += 20;

                // +10 jeśli ma spacje (np. iPhone 14 Pro Max)
                if (line.contains(" ")) score += 10;

                if (score > bestScore) {
                    bestScore = score;
                    best = line;
                }
            }
        }

        return best;
    }

}

