package de.itlobby.discoverj.ui.core;

import de.itlobby.discoverj.ui.viewcontroller.*;

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