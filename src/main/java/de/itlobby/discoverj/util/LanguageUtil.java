package de.itlobby.discoverj.util;

import de.itlobby.discoverj.models.Language;
import de.itlobby.discoverj.settings.AppConfig;
import de.itlobby.discoverj.settings.Settings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public class LanguageUtil {

    private static final Logger log = LogManager.getLogger(LanguageUtil.class);

    private LanguageUtil() {
    }

    public static ResourceBundle getBundle() {
        AppConfig config = Settings.getInstance().getConfig();

        if (config == null || config.getLanguage() == null) {
            Settings.getInstance().saveConfig(new AppConfig());
            config = Settings.getInstance().getConfig();
        }

        Language language = config.getLanguage();
        return readBundleForLanguage(language);
    }

    private static PropertyResourceBundle readBundleForLanguage(Language language) {
        try {
            URL url = SystemUtil.getResourceURL(language.getBundlePath());
            InputStreamReader inputStream = new InputStreamReader(url.openStream(), StandardCharsets.UTF_8);
            return new PropertyResourceBundle(inputStream);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public static String getString(String bundleKey) {
        if (getBundle().containsKey(bundleKey)) {
            return getBundle().getString(bundleKey);
        } else {
            return readBundleForLanguage(Language.ENGLISH).getString(bundleKey);
        }
    }
}
