package pl.gesieniec.gsmseller.phone.scan.parser;

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
        "Starlight", "Space Black", "Pacific Blue",
        "Sunrise Gold", "Alpine Green", "Graphite", "Cream",
        "Onyx", "Coral", "Titanium", "Cobalt Blue"
    );

    private static final Set<String> BLACKLIST = Set.of(
        "pro", "plus", "ultra", "note", "max", "mini",
        "5g", "lte",
        "redmi", "galaxy", "iphone", "xiaomi", "samsung", "apple",
        "gb", "ram", "rom"
    );


    public static String extractColor(String text) {

        String[] lines = text.split("\\R");

        String bestColor = null;
        int bestScore = 0;

        for (String rawLine : lines) {

            String line = rawLine.trim();
            if (line.isEmpty()) continue;

            String lowerLine = line.toLowerCase();

            // rozbij linię na tokeny
            List<String> tokens = new ArrayList<>(List.of(lowerLine.split("\\s+")));

            // usuń blacklistę
            tokens.removeIf(t -> BLACKLIST.contains(t.replaceAll("[^a-z]", "")));

            if (tokens.isEmpty()) continue;

            String cleanedLine = String.join(" ", tokens);

            for (String color : COLOR_DICTIONARY) {

                String lowerColor = color.toLowerCase();
                int score = 0;

                // pełna fraza
                if (cleanedLine.contains(lowerColor)) {
                    score += 100;
                }

                // kolor na końcu
                if (cleanedLine.endsWith(lowerColor)) {
                    score += 40;
                }

                // dopasowanie pojedynczego słowa
                for (String word : lowerColor.split("\\s+")) {
                    if (cleanedLine.matches(".*\\b" + Pattern.quote(word) + "\\b.*")) {
                        score += 20;
                    }
                }

                // ranking kolorów
                score += colorRank(color);

                // linie z cyframi są mniej wiarygodne
                if (line.matches(".*\\d.*")) score -= 15;

                if (score > bestScore) {
                    bestScore = score;
                    bestColor = color;
                }
            }
        }

        return bestColor;
    }



    private static int colorRank(String color) {
        int words = color.split("\\s+").length;

        if (words >= 2) return 100;   // Ocean Blue, Midnight Black
        return 60;                    // Blue, Black
    }

}

