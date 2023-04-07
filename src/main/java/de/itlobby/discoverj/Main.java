package de.itlobby.discoverj;

import de.itlobby.discoverj.services.InitialService;
import de.itlobby.discoverj.services.MainService;
import de.itlobby.discoverj.settings.Settings;
import de.itlobby.discoverj.ui.core.ServiceLocator;
import de.itlobby.discoverj.ui.core.ViewManager;
import de.itlobby.discoverj.util.SystemUtil;
import de.itlobby.discoverj.util.VersionDetector;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.IOException;
import java.util.logging.LogManager;

import static de.itlobby.discoverj.util.SystemUtil.DISCOVERJ_TEMP_DIR;

public class Main {
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(Main.class);

    static {
        // disable java.util.logging
        LogManager.getLogManager().reset();
        // redirect to log4j
        SLF4JBridgeHandler.install();
    }

    static {
        try {
            if (DISCOVERJ_TEMP_DIR.exists()) {
                FileUtils.deleteDirectory(DISCOVERJ_TEMP_DIR);
            }
            DISCOVERJ_TEMP_DIR.mkdir();
        } catch (
                IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public static void main(String[] args) {
        Platform.startup(() -> {
            Settings.getInstance().getConfig();
            SystemUtil.setProxy();

            MainService mainService = ServiceLocator.get(MainService.class);

            Stage primaryStage = new Stage();
            primaryStage.setOnCloseRequest(event -> mainService.exitApplication());

            ViewManager viewManager = ViewManager.getInstance();
            viewManager.setPrimaryStage(primaryStage);
            viewManager.initialize();

            mainService.prepareUIBindings();
            mainService.startMemoryWatchDog();

            VersionDetector.determineCurrentVersion();

            ServiceLocator.get(InitialService.class).openInitialOpenDialog();
        });
    }
}
