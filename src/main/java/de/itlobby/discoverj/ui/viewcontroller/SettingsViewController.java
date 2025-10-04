package de.itlobby.discoverj.ui.viewcontroller;

import static de.itlobby.discoverj.util.LanguageUtil.getString;
import static java.text.MessageFormat.format;

import de.itlobby.discoverj.models.Language;
import de.itlobby.discoverj.searchengines.SearxCoverSearchEngine;
import de.itlobby.discoverj.ui.core.ServiceLocator;
import de.itlobby.discoverj.ui.utils.AwesomeHelper;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import java.util.Optional;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.util.Callback;

public class SettingsViewController implements ViewController {

    public CheckBox chkOverwriteCover;
    public Button btnSave;
    public Button btnCancel;
    public TextField txtMinCoverSize;
    public FlowPane flowPaneSettingsSearchMachineOrder;
    public ComboBox<Language> cmbLanguage;
    public TextField txtProxyUrl;
    public TextField txtProxyPort;
    public TextField txtProxyUser;
    public PasswordField txtProxyPassword;
    public CheckBox chkProxyActive;
    public TextField txtSearchTimeout;
    public TextField txtMaxCoverSize;
    public CheckBox chkOverwriteOnlyHigher;
    public TextField txtLocalAdditionalFolderPath;
    public TextField txtLocalNamePattern;
    public CheckBox chkGeneralManualImageSelection;
    public CheckBox chkGeneralAutoLastAudio;
    public CheckBox chkDiscogsUseYear;
    public CheckBox chkDiscogsUseCountry;
    public TextField txtDiscogsCountry;
    public CheckBox chkGeneralPrimarySingleCover;
    public TextField txtGoogleSearchPattern;
    public HBox buttonLayout;
    public CheckBox chkLocalScanAudiofiles;
    public CheckBox chkLocalMatchAlbum;
    public CheckBox chkLocalMatchAlbumArtist;
    public CheckBox chkLocalMatchYear;
    public VBox layoutAudioFileMatch;
    public TextField txtCustomSearxInstance;
    public CheckBox chkUseCustomSearxInstance;
    public Label txtFindCustomSearxInstance;
    public Button btnCheckHoster;
    public Text txtSearxHosterValid;

    @Override
    public void initialize() {
        initBindings();
        initCellFactories();

        AwesomeHelper.createIconButton(
            btnCheckHoster,
            FontAwesomeIcon.CHECK_SQUARE,
            "default-icon",
            getString("key.settingsview.settings.searx.checkHoster"),
            "24px"
        );
    }

    private void initCellFactories() {
        cmbLanguage.setCellFactory(createLanguageCellFactory());
    }

    private Callback<ListView<Language>, ListCell<Language>> createLanguageCellFactory() {
        return new Callback<>() {
            @Override
            public ListCell<Language> call(ListView<Language> param) {
                return new ListCell<>() {
                    {
                        super.setPrefWidth(100);
                    }

                    @Override
                    public void updateItem(Language item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item != null) {
                            setGraphic(new ImageView(new Image(item.getImagePath())));
                            setText(item.getDisplayValue());
                        } else {
                            setText(null);
                        }
                    }
                };
            }
        };
    }

    private void initBindings() {
        btnSave.prefWidthProperty().bind(buttonLayout.widthProperty().divide(2));
        btnCancel.prefWidthProperty().bind(buttonLayout.widthProperty().divide(2));

        txtProxyUrl.disableProperty().bind(chkProxyActive.selectedProperty().not());
        txtProxyPort.disableProperty().bind(chkProxyActive.selectedProperty().not());
        txtProxyUser.disableProperty().bind(chkProxyActive.selectedProperty().not());
        txtProxyPassword.disableProperty().bind(chkProxyActive.selectedProperty().not());

        chkGeneralAutoLastAudio.disableProperty().bind(chkGeneralPrimarySingleCover.selectedProperty());
        chkGeneralPrimarySingleCover
            .selectedProperty()
            .addListener((observable, oldValue, newValue) -> chkGeneralAutoLastAudio.setSelected(!newValue));

        btnCheckHoster.setOnAction(event -> checkHoster());

        txtLocalNamePattern.disableProperty().bind(txtLocalAdditionalFolderPath.textProperty().isEmpty());

        txtDiscogsCountry.disableProperty().bind(chkDiscogsUseCountry.selectedProperty().not());

        txtCustomSearxInstance.disableProperty().bind(chkUseCustomSearxInstance.selectedProperty().not());
        btnCheckHoster.disableProperty().bind(chkUseCustomSearxInstance.selectedProperty().not());

        layoutAudioFileMatch.disableProperty().bind(chkLocalScanAudiofiles.selectedProperty().not());

        chkOverwriteOnlyHigher.disableProperty().bind(chkOverwriteCover.selectedProperty().not());
        chkOverwriteCover
            .selectedProperty()
            .addListener((observable, oldValue, newValue) -> {
                if (Boolean.FALSE.equals(newValue)) {
                    chkOverwriteOnlyHigher.setSelected(false);
                }
            });
    }

    private void checkHoster() {
        Optional<String> errorMessage = ServiceLocator.get(SearxCoverSearchEngine.class).checkInstance(
            txtCustomSearxInstance.getText()
        );

        if (errorMessage.isEmpty()) {
            txtSearxHosterValid.setText(getString("key.settingsview.settings.searx.validHoster"));
            txtSearxHosterValid.setFill(Paint.valueOf("#008a15"));
        } else {
            txtSearxHosterValid.setText(
                format("{0}:\n{1}", getString("key.settingsview.settings.searx.invalidHoster"), errorMessage.get())
            );
            txtSearxHosterValid.setFill(Paint.valueOf("#c80000"));
        }
    }
}
