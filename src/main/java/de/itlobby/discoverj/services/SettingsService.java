package de.itlobby.discoverj.services;

import de.itlobby.discoverj.listeners.ListenerStateProvider;
import de.itlobby.discoverj.models.Language;
import de.itlobby.discoverj.settings.AppConfig;
import de.itlobby.discoverj.settings.Settings;
import de.itlobby.discoverj.ui.core.ServiceLocator;
import de.itlobby.discoverj.ui.core.ViewManager;
import de.itlobby.discoverj.ui.core.Views;
import de.itlobby.discoverj.ui.viewcontroller.SettingsViewController;
import de.itlobby.discoverj.util.ConfigUtil;
import de.itlobby.discoverj.util.ImageUtil;
import de.itlobby.discoverj.util.LanguageUtil;
import de.itlobby.discoverj.util.SystemUtil;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;

import java.util.List;

public class SettingsService implements Service {
    public void initialize() {
        SettingsViewController viewController = getSettingsViewController();
        viewController.btnCancel.setOnAction(event -> close());
        viewController.btnSave.setOnAction(event -> save());
        viewController.txtFindCustomSearxInstance.setOnMouseClicked(
                event -> SystemUtil.browseUrl("https://searx.space"));

        FlowPane searchEngineLayout = viewController.flowPaneSettingsSearchMachineOrder;

        AppConfig config = Settings.getInstance().getConfig();
        viewController.chkOverwriteCover.setSelected(config.isOverwriteCover());
        viewController.chkOverwriteOnlyHigher.setSelected(config.isOverwriteOnlyHigher());
        viewController.txtMinCoverSize.setText(String.valueOf(config.getMinCoverSize()));
        viewController.txtMaxCoverSize.setText(String.valueOf(config.getMaxCoverSize()));
        viewController.cmbLanguage.getItems().clear();
        viewController.cmbLanguage.getItems().addAll(Language.values());
        viewController.cmbLanguage.setValue(config.getLanguage());
        viewController.txtSearchTimeout.setText(String.valueOf(config.getSearchTimeout()));
        viewController.chkGeneralManualImageSelection.setSelected(config.isGeneralManualImageSelection());
        viewController.chkGeneralAutoLastAudio.setSelected(config.isGeneralAutoLastAudio());
        viewController.chkGeneralPrimarySingleCover.setSelected(config.isPrimarySingleCover());

        viewController.txtGoogleSearchPattern.setText(config.getGoogleSearchPattern());
        viewController.chkUseCustomSearxInstance.setSelected(config.isSearxCustomInstanceActive());
        viewController.txtCustomSearxInstance.setText(config.getSearxCustomInstance());

        viewController.chkDiscogsUseYear.setSelected(config.isDiscogsUseYear());
        viewController.chkDiscogsUseCountry.setSelected(config.isDiscogsUseCountry());
        viewController.txtDiscogsCountry.setText(config.getDiscogsCountry());

        viewController.txtLocalAdditionalFolderPath.setText(config.getLocalAdditionalFolderPath());
        viewController.txtLocalNamePattern.setText(config.getLocalNamePattern());
        viewController.chkLocalScanAudiofiles.setSelected(config.isLocalScanAudioFiles());
        viewController.chkLocalMatchAlbum.setSelected(config.isLocalMatchAlbum());
        viewController.chkLocalMatchAlbumArtist.setSelected(config.isLocalMatchAlbumArtist());
        viewController.chkLocalMatchYear.setSelected(config.isLocalMatchYear());

        viewController.chkProxyActive.setSelected(config.isProxyEnabled());
        viewController.txtProxyUrl.setText(config.getProxyUrl());
        viewController.txtProxyPort.setText(config.getProxyPort());
        viewController.txtProxyUser.setText(config.getProxyUser());
        viewController.txtProxyPassword.setText(config.getProxyPassword());

        searchEngineLayout.getChildren().clear();
        List<ImageView> searchEnginePictures = ImageUtil.createSearchEnginePictures(35, true, searchEngineLayout, true);
        searchEngineLayout.getChildren().addAll(searchEnginePictures);
    }

    private void save() {
        AppConfig appConfig = new AppConfig();
        SettingsViewController viewController = getSettingsViewController();

        appConfig.setOverwriteCover(viewController.chkOverwriteCover.isSelected());
        appConfig.setOverwriteOnlyHigher(viewController.chkOverwriteOnlyHigher.isSelected());
        appConfig.setMinCoverSize(Integer.parseInt(viewController.txtMinCoverSize.getText()));
        appConfig.setMaxCoverSize(Integer.parseInt(viewController.txtMaxCoverSize.getText()));
        appConfig.setSearchEngineList(ConfigUtil.imageToSearchEngine(viewController.flowPaneSettingsSearchMachineOrder.getChildren()));
        int selectedIndex = viewController.cmbLanguage.getSelectionModel().getSelectedIndex();
        appConfig.setLanguage(Language.values()[selectedIndex == -1 ? appConfig.getLanguage().ordinal() : selectedIndex]);
        appConfig.setSearchTimeout(Integer.parseInt(viewController.txtSearchTimeout.getText()));
        appConfig.setGeneralManualImageSelection(viewController.chkGeneralManualImageSelection.isSelected());
        appConfig.setGeneralAutoLastAudio(viewController.chkGeneralAutoLastAudio.isSelected());
        appConfig.setPrimarySingleCover(viewController.chkGeneralPrimarySingleCover.isSelected());

        appConfig.setGoogleSearchPattern(viewController.txtGoogleSearchPattern.getText());
        appConfig.setSearxCustomInstanceActive(viewController.chkUseCustomSearxInstance.isSelected());
        appConfig.setSearxCustomInstance(viewController.txtCustomSearxInstance.getText());

        appConfig.setDiscogsUseYear(viewController.chkDiscogsUseYear.isSelected());
        appConfig.setDiscogsUseCountry(viewController.chkDiscogsUseCountry.isSelected());
        appConfig.setDiscogsCountry(viewController.txtDiscogsCountry.getText());

        appConfig.setLocalAdditionalFolderPath(viewController.txtLocalAdditionalFolderPath.getText());
        appConfig.setLocalNamePattern(viewController.txtLocalNamePattern.getText());
        appConfig.setLocalScanAudioFiles(viewController.chkLocalScanAudiofiles.isSelected());
        appConfig.setLocalMatchAlbum(viewController.chkLocalMatchAlbum.isSelected());
        appConfig.setLocalMatchAlbumArtist(viewController.chkLocalMatchAlbumArtist.isSelected());
        appConfig.setLocalMatchYear(viewController.chkLocalMatchYear.isSelected());

        appConfig.setProxyEnabled(viewController.chkProxyActive.isSelected());
        appConfig.setProxyUrl(viewController.txtProxyUrl.getText());
        appConfig.setProxyPort(viewController.txtProxyPort.getText());
        appConfig.setProxyUser(viewController.txtProxyUser.getText());
        appConfig.setProxyPassword(viewController.txtProxyPassword.getText());

        checkForRestart();

        Settings.getInstance().saveConfig(appConfig);
        close();
        ListenerStateProvider.getInstance().getSettingsSavedListener().saved();
    }

    private void checkForRestart() {
        Language fromCombo = getSettingsViewController().cmbLanguage.getSelectionModel().getSelectedItem();
        Language fromConfig = Settings.getInstance().getConfig().getLanguage();

        if (fromCombo == null || fromConfig == null) {
            return;
        }

        if (!fromCombo.equals(fromConfig)) {
            ServiceLocator.get(LightBoxService.class)
                    .showTextDialog(LanguageUtil.getString("InitialController.warning"),
                            LanguageUtil.getString("SettingsController.SettingsChangesNeedRestart"), false
                    );
        }
    }

    private void close() {
        ViewManager.getInstance().closeView(Views.SETTINGS_VIEW);
    }
}
