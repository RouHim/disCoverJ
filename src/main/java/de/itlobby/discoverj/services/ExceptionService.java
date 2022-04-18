package de.itlobby.discoverj.services;

import de.itlobby.discoverj.framework.ServiceLocator;
import de.itlobby.discoverj.util.LanguageUtil;
import de.itlobby.discoverj.util.SystemUtil;
import de.itlobby.discoverj.viewcontroller.MainViewController;
import javafx.application.Platform;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ExceptionService implements Service, Thread.UncaughtExceptionHandler {
    private final Logger log = LogManager.getLogger(this.getClass());


    @Override
    public void uncaughtException(Thread t, Throwable throwable) {
        Exception exception = new Exception(throwable);
        MainViewController mainViewController = getMainViewController();

        Platform.runLater(() -> {
            mainViewController.hideBusyIndicator();

            log.error(exception.getMessage(), exception);

            LightBoxService lightBoxService = ServiceLocator.get(LightBoxService.class);

            if (exception.getCause() instanceof OutOfMemoryError) {
                showOutOfMemoryInfo(lightBoxService);
            } else {
                lightBoxService.showTextDialog(
                        LanguageUtil.getString("exceptionService.errorOccurredTitle"),
                        LanguageUtil.getString("exceptionService.errorOccurredMsg") + "\n" + exception.getMessage()
                );
            }
        });
    }

    public void showOutOfMemoryInfo(LightBoxService lightBoxService) {
        String msg = LanguageUtil.getString("exceptionService.outOfMemoryMsg");

        lightBoxService.showDialog(
                LanguageUtil.getString("exceptionService.outOfMemoryTitle"),
                new TextFlow(new Text(msg)),
                lightBoxService::hideDialog,
                () -> {
                    //TODO: neue anleitung finden
                    SystemUtil.browseUrl("www.it-lobby.de/discoverj-anpassung-der-ram-zuweisung/");
                    lightBoxService.hideDialog();
                },
                false
        );
    }
}
