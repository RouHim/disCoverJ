package de.itlobby.discoverj;

import de.itlobby.discoverj.framework.ServiceLocator;
import de.itlobby.discoverj.framework.ViewManager;
import de.itlobby.discoverj.services.InitialService;
import de.itlobby.discoverj.services.MainService;
import de.itlobby.discoverj.settings.Settings;
import de.itlobby.discoverj.util.SystemUtil;
import de.itlobby.discoverj.util.VersionDetector;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.util.logging.LogManager;

public class Main {
    static {
        // disable java.util.logging
        LogManager.getLogManager().reset();
        // redirect to log4j
        SLF4JBridgeHandler.install();
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
