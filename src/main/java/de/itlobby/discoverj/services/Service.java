package de.itlobby.discoverj.services;

import de.itlobby.discoverj.framework.ViewManager;
import de.itlobby.discoverj.framework.Views;
import de.itlobby.discoverj.viewcontroller.MainViewController;
import de.itlobby.discoverj.viewcontroller.SettingsViewController;

public interface Service {
    default MainViewController getMainViewController() {
        return ViewManager.getInstance().getViewController(Views.MAIN_VIEW);
    }

    default SettingsViewController getSettingsViewController() {
        return ViewManager.getInstance().getViewController(Views.SETTINGS_VIEW);
    }
}
