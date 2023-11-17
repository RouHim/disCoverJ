package de.itlobby.discoverj.ui.core;

import de.itlobby.discoverj.ui.viewcontroller.CoverDetailViewController;
import de.itlobby.discoverj.ui.viewcontroller.ImageSelectionViewController;
import de.itlobby.discoverj.ui.viewcontroller.MainViewController;
import de.itlobby.discoverj.ui.viewcontroller.MultiselectionLayoutViewController;
import de.itlobby.discoverj.ui.viewcontroller.OpenFileViewController;
import de.itlobby.discoverj.ui.viewcontroller.SettingsViewController;
import de.itlobby.discoverj.ui.viewcontroller.ViewController;

public enum Views {
    MAIN_VIEW("views/MainView.fxml", "views.mainView", MainViewController.class),
    SETTINGS_VIEW("views/SettingsView.fxml", "views.settingsView", SettingsViewController.class),
    COVER_DETAIL_VIEW("views/CoverDetailView.fxml", "views.coverDetailView", CoverDetailViewController.class),
    DROP_FILE_VIEW("views/DropFileView.fxml", "", OpenFileViewController.class),
    MULTI_SELECTION_LAYOUT_VIEW("views/MuliselectionLayoutView.fxml", "", MultiselectionLayoutViewController.class),
    IMAGE_SELECTION_VIEW("views/ImageSelectionView.fxml", "", ImageSelectionViewController.class);

    private final String path;
    private final Class<ViewController> clazz;
    private final String title;

    @SuppressWarnings("unchecked")
    Views(String path, String title, Class<?> clazz) {
        this.path = path;
        this.title = title;
        this.clazz = (Class<ViewController>) clazz;
    }

    public String getPath() {
        return path;
    }

    public String getTitle() {
        return title;
    }

    public Class<ViewController> getClazz() {
        return clazz;
    }
}