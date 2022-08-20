package de.itlobby.discoverj.services;

import de.itlobby.discoverj.listeners.ActionListener;
import de.itlobby.discoverj.listeners.ListenerStateProvider;
import de.itlobby.discoverj.settings.Settings;
import de.itlobby.discoverj.ui.core.ServiceLocator;
import de.itlobby.discoverj.ui.core.ViewManager;
import de.itlobby.discoverj.ui.core.Views;
import de.itlobby.discoverj.ui.viewcontroller.MainViewController;
import de.itlobby.discoverj.util.LanguageUtil;
import de.itlobby.discoverj.util.SystemUtil;
import de.itlobby.discoverj.util.helper.ImageCache;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;

import java.awt.*;
import java.io.File;
import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;

public class MainService implements Service {
    private static final Logger log = LogManager.getLogger(MainService.class);
    private Timer memCheckTimer;

    public void prepareUIBindings() {
        //Open Settings with its own Controller
        MainViewController viewController = getMainViewController();
        viewController.btnOpenSettings.setOnAction(event -> showSettings());
        viewController.btnExitApp.setOnAction(event -> exitApplication());
        viewController.btnOpenAbout.setOnAction(event -> openAbout());
        viewController.btnDonate.setOnAction(event -> donate());
        viewController.btnHelpTranslate.setOnAction(event -> helpTranslate());
        viewController.btnSendFeedback.setOnAction(event -> sendFeedback());
        viewController.btnReportBug.setOnAction(event -> reportBug());
        ListenerStateProvider.getInstance().setSettingsSavedListener(SystemUtil::setProxy);

        //actions of audio detail
        InitialService initialService = ServiceLocator.get(InitialService.class);
        viewController.btnRemoveCover.setOnAction(event -> initialService.removeLastSelectedCover());
        viewController.btnCopyCoverToClipBrd.setOnAction(event -> initialService.copyCoverToClipBrd());
        viewController.btnOpenGoogleImageSearch.setOnAction(event -> initialService.searchOnGoogleImages());
        viewController.btnFindFolder.setOnAction(event -> initialService.findFolder());
        viewController.imgCurrentCover.setOnMouseClicked(event -> initialService.showCurrentCoverDetailed());

        ServiceLocator.get(DragDropService.class).initDragAndDrop();
    }

    private void helpTranslate() {
        SystemUtil.browseUrl("https://crowdin.com/project/discoverj/invite");
    }

    private void reportBug() {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        FileAppender fileAppender = ctx.getConfiguration().getAppender("file");
        File logFile = new File(fileAppender.getFileName());

        LightBoxService lightBoxService = ServiceLocator.get(LightBoxService.class);
        String msg = LanguageUtil.getString("mainService.reportBugMsg");
        TextFlow txtIntro = new TextFlow(new Text(String.format(msg, logFile.getAbsolutePath())));
        HBox contentLayout = new HBox(txtIntro);
        HBox.setMargin(txtIntro, new Insets(5));

        ActionListener okListener = () ->
        {
            SystemUtil.browseLocalPath(logFile);
            lightBoxService.hideDialog();
        };
        ActionListener cancelListener = lightBoxService::hideDialog;

        lightBoxService.showDialog(LanguageUtil.getString("mainService.reportBugTitle"), contentLayout, cancelListener, okListener, false, false);

        SystemUtil.browseUrl("https://github.com/RouHim/disCoverJ/issues/new");
    }

    public void startMemoryWatchDog() {
        memCheckTimer = new Timer();
        memCheckTimer.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        memoryCheckTick();
                    }
                }, 0, 5000
        );
    }

    private void memoryCheckTick() {
        Runtime runtime = Runtime.getRuntime();
        int mb = 1024 * 1024;
        long maxMem = runtime.maxMemory() / mb;
        long curMem = (runtime.totalMemory() - runtime.freeMemory()) / mb;
        double percentage = (double) curMem / (double) maxMem;

        Platform.runLater(() -> {
            getMainViewController().txtJavaCurrentMem.setText(curMem + "");
            getMainViewController().txtJavaMaxMem.setText(maxMem + "M");
            getMainViewController().pbJavaMemory.setProgress(percentage);
        });
    }

    private void sendFeedback() {
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;

        new Thread(() -> {
            if (desktop != null && desktop.isSupported(Desktop.Action.MAIL)) {
                try {
                    URI emailURL = new URIBuilder()
                            .setScheme("mailto")
                            .setPath("rouven@himmelstein.info")
                            .build();

                    desktop.mail(emailURL);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            } else {
                log.info("Send mail to rouven(at)himmelstein.info");
            }
        }).start();
    }

    private void donate() {
        SystemUtil.browseUrl("https://www.paypal.me/disCoverJ");
    }

    private void showSettings() {
        ViewManager.getInstance().showViewAsStage(Views.SETTINGS_VIEW);
        ServiceLocator.get(SettingsService.class).initialize();
    }

    public void exitApplication() {
        Platform.runLater(() -> {
            ServiceLocator.get(CoverPersistentService.class).cleanup();

            ServiceLocator.get(InitialService.class).setInterruptProgress(true);
            ServiceLocator.get(SearchService.class).setInterruptProgress(true);

            if (memCheckTimer != null) {
                memCheckTimer.cancel();
                memCheckTimer.purge();
                memCheckTimer = null;
            }

            Platform.exit();
        });
    }

    private void openAbout() {
        String msg =
                "disCoverJ\n" +
                        "Version: " + Settings.getInstance().getVersion().toString() + "\n" +
                        LanguageUtil.getString("key.mainwindow.about.developed");

        ServiceLocator.get(LightBoxService.class).showTextDialog(LanguageUtil.getString("key.mainview.menu.about"), msg);
    }

    public void handleSearchThreadExecption(Throwable e) {
        if (e instanceof OutOfMemoryError) {
            ImageCache.getInstance().clear();
            ServiceLocator.unload(InitialService.class);
            ServiceLocator.unload(SearchService.class);

            ServiceLocator.get(LightBoxService.class).showTextDialog("Error", e.getMessage());
        } else {
            log.error(e.getMessage(), e);
        }
    }
}
