package de.itlobby.discoverj.ui.core;

import de.itlobby.discoverj.listeners.KeyPressedListener;
import de.itlobby.discoverj.util.LanguageUtil;
import de.itlobby.discoverj.ui.viewcontroller.ViewController;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCodeCombination;
import javafx.stage.Stage;

public class ViewManager {
    private static ViewManager instance;
    private Stage primaryStage;
    private FXMLFactory fxmlFactory;

    private ViewManager() {
    }

    public static ViewManager getInstance() {
        if (instance == null) {
            instance = new ViewManager();
        }

        return instance;
    }

    public void initialize() {
        fxmlFactory = new FXMLFactory();

        activateView(Views.MAIN_VIEW);

        primaryStage.setHeight(750);
        primaryStage.setWidth(750);
        primaryStage.getIcons().add(new Image("icon/icon.png"));
        primaryStage.show();
    }

    public void activateView(Views viewToLoad) {
        if (primaryStage != null) {
            Scene scene = fxmlFactory.getView(viewToLoad);
            primaryStage.setScene(scene);
            primaryStage.setTitle(LanguageUtil.getString(viewToLoad.getTitle()));
            fxmlFactory.getViewController(viewToLoad, viewToLoad.getClazz()).initialize();
        }
    }

    public <T extends ViewController> T getViewController(Views view) {
        return (T) (view.getClazz()).cast(fxmlFactory.getViewController(view, view.getClazz()));
    }

    public Parent createLayoutFromView(Views view) {
        return fxmlFactory.createLayoutFromView(view);
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public void registerKeyPressedListener(Views view, KeyCodeCombination keyCodeCombination, KeyPressedListener keyPressedListener) {
        this.fxmlFactory.getView(view).getAccelerators().put(
                keyCodeCombination,
                keyPressedListener::keyPressed
        );
    }

    public void showViewAsStage(Views viewToShow) {
        Scene scene = fxmlFactory.getView(viewToShow);

        Stage stage = new Stage();
        stage.getIcons().add(new Image("icon/icon.png"));
        stage.setScene(scene);
        stage.setTitle(LanguageUtil.getBundle().getString(viewToShow.getTitle()));
        stage.show();
    }

    public void closeView(Views viewToClose) {
        Scene view = getView(viewToClose);
        Stage stage = (Stage) view.getWindow();
        stage.close();
    }

    private Scene getView(Views view) {
        return fxmlFactory.getView(view);
    }

    public void setPrimaryTitle(String title) {
        primaryStage.setTitle(title);
    }
}
