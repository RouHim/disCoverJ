package de.itlobby.discoverj.settings;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.itlobby.discoverj.models.Version;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Settings {

    private static final Logger log = LogManager.getLogger(Settings.class);
    private static final String CONFIG_FILE_NAME = "disCoverJ.json";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final AtomicReference<Settings> instance = new AtomicReference<>();

    private Version version;
    private boolean coverLoadingDisabled;
    private AppConfig appConfig;

    private Settings() {}

    public static Settings getInstance() {
        if (instance.get() == null) {
            instance.set(new Settings());
        }
        return instance.get();
    }

    private File getPropertiesFile() {
        String userHome = System.getProperty("user.home");
        File userHomeConfigFilePath = Paths.get(userHome, ".config", "disCoverJ").toFile();
        userHomeConfigFilePath.mkdirs();
        return new File(userHomeConfigFilePath, CONFIG_FILE_NAME);
    }

    public AppConfig getConfig() {
        if (appConfig == null) {
            appConfig = loadConfig();
        }

        return appConfig;
    }

    private AppConfig loadConfig() {
        AppConfig localAppConfig = new AppConfig();

        try {
            File propFile = getPropertiesFile();

            if (propFile.exists()) {
                localAppConfig = readProperties(propFile);
            } else {
                saveConfig(localAppConfig);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return localAppConfig;
    }

    private AppConfig readProperties(File propFile) throws IOException {
        return objectMapper.readValue(propFile, AppConfig.class);
    }

    public void saveConfig(AppConfig appConfig) {
        try {
            this.appConfig = appConfig;
            File propFile = getPropertiesFile();
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(propFile, appConfig);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public Version getVersion() {
        return version;
    }

    public void setVersion(Version version) {
        this.version = version;
    }

    public boolean isCoverLoadingDisabled() {
        return coverLoadingDisabled;
    }

    public void setCoverLoadingDisabled(boolean coverLoadingDisabled) {
        this.coverLoadingDisabled = coverLoadingDisabled;
    }
}
