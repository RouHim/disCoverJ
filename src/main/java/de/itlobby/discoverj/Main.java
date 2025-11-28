package de.itlobby.discoverj;

import static de.itlobby.discoverj.util.SystemUtil.DISCOVERJ_TEMP_DIR;

import de.itlobby.discoverj.services.InitialService;
import de.itlobby.discoverj.services.MainService;
import de.itlobby.discoverj.settings.Settings;
import de.itlobby.discoverj.ui.core.ServiceLocator;
import de.itlobby.discoverj.ui.core.ViewManager;
import de.itlobby.discoverj.util.SystemUtil;
import de.itlobby.discoverj.util.VersionDetector;
import java.io.IOException;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;

public class Main {

  static void main(String[] args) throws IOException {
    if (DISCOVERJ_TEMP_DIR.exists()) {
      FileUtils.deleteDirectory(DISCOVERJ_TEMP_DIR);
    }

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
