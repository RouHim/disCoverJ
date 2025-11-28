package de.itlobby.discoverj.services;

import de.itlobby.discoverj.ui.core.ViewManager;
import de.itlobby.discoverj.ui.core.Views;
import de.itlobby.discoverj.ui.viewcontroller.MainViewController;
import de.itlobby.discoverj.ui.viewcontroller.SettingsViewController;

public interface Service {
  default MainViewController getMainViewController() {
    return ViewManager.getInstance().getViewController(Views.MAIN_VIEW);
  }

  default SettingsViewController getSettingsViewController() {
    return ViewManager.getInstance().getViewController(Views.SETTINGS_VIEW);
  }
}
