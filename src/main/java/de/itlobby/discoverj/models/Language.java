package de.itlobby.discoverj.models;

import java.util.Arrays;
import java.util.Locale;

public enum Language {
    //To add a new language just add translated .properties file & add new enum constant
    ENGLISH(Locale.US, "languages/disCoverJ_en.properties", "/images/flags/us.png"),
    GERMAN(Locale.GERMAN, "languages/disCoverJ_de.properties", "/images/flags/de.png"),
    SPANISH(Locale.forLanguageTag("es-ES"), "languages/disCoverJ_es.properties", "/images/flags/es.png"),
    HEBREW(Locale.forLanguageTag("he"), "languages/disCoverJ_he.properties", "/images/flags/he.png");

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
