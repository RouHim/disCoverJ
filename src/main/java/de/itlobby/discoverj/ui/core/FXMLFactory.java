package de.itlobby.discoverj.ui.core;

import de.itlobby.discoverj.ui.viewcontroller.ViewController;
import de.itlobby.discoverj.util.LanguageUtil;
import de.itlobby.discoverj.util.SystemUtil;
import java.io.IOException;
import java.net.URL;
import java.util.EnumMap;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FXMLFactory {

  private final EnumMap<Views, Scene> viewList;
  private final EnumMap<Views, ViewController> viewControllerList;
  private final Logger log = LogManager.getLogger(this.getClass());

  public FXMLFactory() {
    viewList = new EnumMap<>(Views.class);
    viewControllerList = new EnumMap<>(Views.class);
  }

  public Scene getView(Views viewToLoad) {
    Scene scene = viewList.get(viewToLoad);

    if (scene == null) {
      scene = createView(viewToLoad);
    }

    return scene;
  }

  protected Parent createLayoutFromView(Views viewToLoad) {
    URL url = SystemUtil.getResourceURL(viewToLoad.getPath());

    try {
      FXMLLoader loader = new FXMLLoader();
      loader.setLocation(url);
      loader.setResources(LanguageUtil.getBundle());

      Parent parent = loader.load();
      ViewController controller = loader.getController();

      viewControllerList.put(viewToLoad, controller);

      return parent;
    } catch (IOException e) {
      log.error(e.getMessage(), e);
    }

    return null;
  }

  private Scene createView(Views viewToLoad) {
    URL url = SystemUtil.getResourceURL(viewToLoad.getPath());

    try {
      FXMLLoader loader = new FXMLLoader();
      loader.setLocation(url);
      loader.setResources(LanguageUtil.getBundle());

      Parent parent = loader.load();
      Scene scene = new Scene(parent);
      ViewController controller = loader.getController();

      viewList.put(viewToLoad, scene);
      viewControllerList.put(viewToLoad, controller);

      return scene;
    } catch (IOException e) {
      log.error(e.getMessage(), e);
    }

    return null;
  }

  public <T extends ViewController> T getViewController(
    Views view,
    Class<T> clazz
  ) {
    return clazz.cast(viewControllerList.get(view));
  }

  public void putViewController(Views key, ViewController value) {
    viewControllerList.put(key, value);
  }
}
