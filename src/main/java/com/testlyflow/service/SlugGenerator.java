package com.testlyflow.service;

import java.util.Map;

/** Transliterates Cyrillic (and arbitrary) text into a URL-safe, ASCII, hyphenated slug. */
public final class SlugGenerator {

    private static final Map<Character, String> CYRILLIC = Map.ofEntries(
            Map.entry('а', "a"), Map.entry('б', "b"), Map.entry('в', "v"), Map.entry('г', "g"),
            Map.entry('д', "d"), Map.entry('е', "e"), Map.entry('ё', "e"), Map.entry('ж', "zh"),
            Map.entry('з', "z"), Map.entry('и', "i"), Map.entry('й', "y"), Map.entry('к', "k"),
            Map.entry('л', "l"), Map.entry('м', "m"), Map.entry('н', "n"), Map.entry('о', "o"),
            Map.entry('п', "p"), Map.entry('р', "r"), Map.entry('с', "s"), Map.entry('т', "t"),
            Map.entry('у', "u"), Map.entry('ф', "f"), Map.entry('х', "h"), Map.entry('ц', "ts"),
            Map.entry('ч', "ch"), Map.entry('ш', "sh"), Map.entry('щ', "sch"), Map.entry('ъ', ""),
            Map.entry('ы', "y"), Map.entry('ь', ""), Map.entry('э', "e"), Map.entry('ю', "yu"),
            Map.entry('я', "ya"));

    private SlugGenerator() {
    }

    public static String slugify(String input) {
        if (input == null) {
            return "";
        }
        StringBuilder transliterated = new StringBuilder();
        for (char ch : input.toLowerCase().toCharArray()) {
            String mapped = CYRILLIC.get(ch);
            transliterated.append(mapped != null ? mapped : ch);
        }
        String slug = transliterated.toString()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return slug.isBlank() ? "category" : slug;
    }
}
