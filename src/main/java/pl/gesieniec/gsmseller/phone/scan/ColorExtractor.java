package pl.gesieniec.gsmseller.phone.scan;

import java.util.*;
import java.util.regex.*;

public class ColorExtractor {

    private static final List<String> COLOR_DICTIONARY = List.of(
        "Black", "White", "Blue", "Red", "Green", "Yellow", "Purple", "Pink",
        "Orange", "Gold", "Silver", "Gray", "Grey", "Brown", "Beige",
        "Midnight Black", "Midnight Blue", "Sky Blue", "Deep Purple",
        "Starry Black", "Forest Green", "Electric Blue", "Polar White",
        "Cosmic Black", "Lunar Blue", "Marine Blue", "Stone Black",
        "Mint Green", "Champagne Gold", "Rose Gold",
        "Starlight", "Space Black", "Pacific Blue", "Midnight",
        "Sunrise Gold", "Alpine Green", "Graphite", "Cream",
        "Onyx", "Coral", "Titanium", "Cobalt Blue"
    );

    public static String extractColor(String text) {

        String[] lines = text.split("\\R");

        Pattern colorPattern = Pattern.compile("^[A-Z][a-z]+(?:\\s+[A-Z][a-z]+){0,2}$");

        String best = null;
        int bestScore = 0;

        for (String rawLine : lines) {

            String line = rawLine.trim();
            if (line.isEmpty()) continue;

            Matcher matcher = colorPattern.matcher(line);
            if (!matcher.matches()) continue;

            int score = 0;

            // dopasowanie do znanej listy kolorów
            for (String c : COLOR_DICTIONARY) {
                if (line.equalsIgnoreCase(c)) {
                    score += 40;
                }
                // fuzzy match (pierwsze słowo zwykle wystarcza)
                if (line.toLowerCase().contains(c.toLowerCase().split(" ")[0])) {
                    score += 25;
                }
            }

            // 2 słowa → typowy kolor marketingowy
            if (line.split("\\s+").length == 2) score += 20;

            // typowe słowa kolorów
            if (line.matches(".*(Black|White|Blue|Silver|Gold|Gray|Green|Red|Purple).*")) {
                score += 20;
            }

            if (score > bestScore) {
                bestScore = score;
                best = line;
            }
        }

        return best;
    }

}

