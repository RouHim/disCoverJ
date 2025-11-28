package de.itlobby.discoverj.models;

import java.util.Arrays;
import java.util.Locale;

public enum Language {
    // Language properties file edit hint: Do NOT use intelliJ to edit properties file, use vs code
    //
    // To add a new language just add translated .properties file & add new enum constant & image
    // Get flags from here: https://hampusborgos.github.io/country-flags/ e.g.: https://raw.githubusercontent.com/hampusborgos/country-flags/main/png100px/ad.png
    // Convert with this command:
    //      for file in *.png; do convert "$file" -resize 32x32\> -gravity center -background transparent -extent 32x32 "$file"; done
    ENGLISH(Locale.US, "languages/disCoverJ_en.properties", "/images/flags/us.png"),
    GERMAN(Locale.GERMAN, "languages/disCoverJ_de.properties", "/images/flags/de.png"),
    SPANISH(Locale.forLanguageTag("es-ES"), "languages/disCoverJ_es.properties", "/images/flags/es.png"),
    HEBREW(Locale.forLanguageTag("he"), "languages/disCoverJ_he.properties", "/images/flags/il.png"),
    CHINESE(Locale.SIMPLIFIED_CHINESE, "languages/disCoverJ_zh.properties", "/images/flags/cn.png"),
    HINDI(Locale.of("hi", "IN"), "languages/disCoverJ_hi.properties", "/images/flags/in.png");

    private final String imagePath;
    private final Locale locale;
    private final String bundlePath;

    Language(Locale locale, String bundlePath, String imagePath) {
        this.locale = locale;
        this.bundlePath = bundlePath;
        this.imagePath = imagePath;
    }

    public static Language fromLocale(Locale toFind) {
        return Arrays.stream(values())
                .filter(language -> language.getLocale().equals(toFind))
                .findFirst()
                .orElse(Language.ENGLISH);
    }

    public Locale getLocale() {
        return locale;
    }

    public String getBundlePath() {
        return bundlePath;
    }

    public String getImagePath() {
        return imagePath;
    }

    public String getDisplayValue() {
        return locale.getDisplayName(Locale.US);
    }

    @Override
    public String toString() {
        return getDisplayValue();
    }
}
