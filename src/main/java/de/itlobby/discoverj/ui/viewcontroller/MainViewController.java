package de.itlobby.discoverj.ui.viewcontroller;

import de.itlobby.discoverj.listeners.ActionListener;
import de.itlobby.discoverj.listeners.ListenerStateProvider;
import de.itlobby.discoverj.listeners.MultipleSelectionListener;
import de.itlobby.discoverj.listeners.ParentKeyDeletedListener;
import de.itlobby.discoverj.mixcd.MixCd;
import de.itlobby.discoverj.models.AudioWrapper;
import de.itlobby.discoverj.models.ImageFile;
import de.itlobby.discoverj.services.CoverSearchService;
import de.itlobby.discoverj.services.InitialService;
import de.itlobby.discoverj.services.LightBoxService;
import de.itlobby.discoverj.services.SelectionService;
import de.itlobby.discoverj.ui.components.AudioListEntry;
import de.itlobby.discoverj.ui.components.FolderListEntry;
import de.itlobby.discoverj.ui.core.ServiceLocator;
import de.itlobby.discoverj.ui.core.ViewManager;
import de.itlobby.discoverj.ui.core.Views;
import de.itlobby.discoverj.ui.utils.AnimationHelper;
import de.itlobby.discoverj.ui.utils.AwesomeHelper;
import de.itlobby.discoverj.ui.utils.GlyphsDude;
import de.itlobby.discoverj.util.AudioUtil;
import de.itlobby.discoverj.util.ImageUtil;
import de.itlobby.discoverj.util.LanguageUtil;
import de.itlobby.discoverj.util.StringUtil;
import de.itlobby.discoverj.util.SystemUtil;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.materialicons.MaterialIcon;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class MainViewController implements ViewController, MultipleSelectionListener, ParentKeyDeletedListener {

    private static final Logger log = LogManager.getLogger(MainViewController.class);
    private static final String MENU_ICON = "menu-icon";
    private static final String DEFAULT_ICON = "default-icon";
    private static final String DEFAULT_ICON_TEXT = "default-icon-text";
    private static final String AUDIO_LINE_SELECTED = "audio-line-selected";

    public AnchorPane rootLayout;
    public GridPane audioDetailsLayout;
    public Button btnFindFolder;
    public Label txtTotalAudioCount;
    public Label txtWithCoverAudioCount;
    public Label txtFilename;
    public Label txtArtist;
    public Label txtTitle;
    public Label txtAlbum;
    public ImageView imgCurrentCover;
    public ProgressBar pbStatus;
    public Label txtState;
    public Label txtCurrentState;
    public Label txtMaxState;
    public Label txtCurrentAudioCoverRes;
    public HBox lightBoxLayout;
    public ProgressBar pbJavaMemory;
    public Label txtJavaCurrentMem;
    public Label txtJavaMaxMem;
    public ProgressIndicator progressScanIndicator;
    public HBox hboxMenubar;
    public TextFlow txtAudioToLoad;

    public Button btnOpenSettings;
    public Button btnExitApp;
    public Button btnOpenAbout;
    public Button btnDonate;
    public Button btnReportBug;
    public VBox lwAudioList;
    public ListView<AudioWrapper> audioList;
    public ScrollPane spAudioList;
    public Button btnActionCircle;
    public Button btnActionCircleIcon;
    public Button btnRemoveCover;
    public Button btnCopyCoverToClipBrd;
    public Button btnPasteCoverFromClipBrd;
    public Label txtIsMixCD;
    public HBox multiSelectionLayout;
    public Button btnOpenGoogleImageSearch;

    private double totalAudioCount;
    private AtomicInteger currentAudioCount;
    private Text txtUpcountIndicatorValue;
    private long lastEventExcecFireTime = 0;

    private ContextMenu audioListContextMenu;
    private double progressMax;
    private double progressCurrent;

    private static void createFadeAnimation(ImageView imageView) {
        FadeTransition ft = new FadeTransition(Duration.millis(500), imageView);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    public void setNewCoverPreview(WritableImage fxImage, int width, int height) {
        Platform.runLater(() -> {
            imgCurrentCover.setImage(fxImage);
            txtCurrentAudioCoverRes.setText(String.format("(%dx%d)", width, height));
        });
    }

    public void activateSearchState(EventHandler<ActionEvent> stopEventHandler, int audioFilesCount) {
        activateActionButton(stopEventHandler, FontAwesomeIcon.TIMES);
        txtMaxState.setText(String.valueOf(audioFilesCount));
        setProgress(0, audioFilesCount);
        unhighlightAll();
    }

    @Override
    public void initialize() {
        createIconButtons();
        initContextMenu();
        deactivateActionButton();
        createBinding();
        registerKeyListener();
        registerGeneralListener();

        ListenerStateProvider.getInstance().setMultipleSelectionListnener(this);
        ListenerStateProvider.getInstance().setParentKeyDeletedListener(this);
    }

    public void showScanResult(
            int withCoverCount,
            int audioFilesCount,
            Map<String, List<AudioWrapper>> audioDirectory
    ) {
        Platform.runLater(() -> {
            tryClearLeftSide();

            hideBusyIndicator();

            setState(MessageFormat.format(LanguageUtil.getString("InitialController.thereAre0Files"), audioFilesCount));

            int withoutCoverCount = audioFilesCount - withCoverCount;

            txtTotalAudioCount.setText(String.valueOf(audioFilesCount));
            txtWithCoverAudioCount.setText(String.valueOf(withCoverCount));

            log.info(String.format("With cover: %s\tWithout cover: %s", withCoverCount, withoutCoverCount));

            setAudioList(audioDirectory);

            if (audioFilesCount <= 0) {
                deactivateActionButton();
            } else {
                activateActionButton(
                        event -> ServiceLocator.get(CoverSearchService.class).search(),
                        FontAwesomeIcon.SEARCH
                );
            }
        });
    }

    /**
     * We do this, because the listview is not always cleared correctly and throws duplicate exceptions.
     */
    private void tryClearLeftSide() {
        int attempts = 0;
        while (attempts < 10) {
            try {
                lwAudioList.getChildren().clear();
                break;
            } catch (Exception e) {
                log.error("Attempt #{} to clear audio list failed", attempts);
                attempts++;
                SystemUtil.threadSleep(100);
            }
        }
    }

    public void showAudioInfo(AudioWrapper audioWrapper, boolean showCover) {
        if (audioWrapper == null) {
            resetRightSide();
            return;
        }

        Platform.runLater(() -> {
            String fileSize = StringUtil.sizeToHumanReadable(audioWrapper.getFileLength());
            txtFilename.setText(String.format("%s (%s)", audioWrapper.getFileName(), fileSize));
            txtTitle.setText(audioWrapper.getTitle());
            txtArtist.setText(audioWrapper.getArtist());
            txtAlbum.setText(audioWrapper.getAlbum());
        });

        // Start new thread, and set mix cd info
        Thread.ofVirtual().start(() -> {
            boolean isMixCD = MixCd.isMixCd(audioWrapper.getParentFilePath());
            Platform.runLater(() -> txtIsMixCD.setVisible(isMixCD));
        });

        if (!showCover) {
            return;
        }

        Thread.ofVirtual().start(() -> {
            Optional<Image> maybeCover = AudioUtil.getCover(audioWrapper.getFilePath());
            Platform.runLater(() -> {
                if (maybeCover.isPresent()) {
                    imgCurrentCover.setImage(maybeCover.get());
                    txtCurrentAudioCoverRes.setText(AudioUtil.getImageResolutionString(maybeCover.get()));
                } else {
                    imgCurrentCover.setImage(null);
                    txtCurrentAudioCoverRes.setText(null);
                }
            });
        });
    }

    public void setProgress(int i, int audioFileListSize) {
        Platform.runLater(() -> {
            this.progressMax = audioFileListSize;
            this.progressCurrent = (i + 1);

            double value = progressCurrent / audioFileListSize;
            pbStatus.setProgress(value);
            txtCurrentState.setText(String.valueOf(i + 1));
        });
    }

    public void increaseProgress() {
        Platform.runLater(() -> {
            progressCurrent = progressCurrent + 1;
            pbStatus.setProgress(progressCurrent / progressMax);
            txtCurrentState.setText(String.valueOf((int) progressCurrent));
        });
    }

    private void initContextMenu() {
        audioListContextMenu = new ContextMenu();
        InitialService initialService = ServiceLocator.get(InitialService.class);

        MenuItem alCmItemSearchSearXImages = new MenuItem(
                LanguageUtil.getString("key.mainview.cm.searx.image.search"),
                GlyphsDude.createIcon(FontAwesomeIcon.SEARCH_PLUS)
        );
        alCmItemSearchSearXImages.setOnAction(e -> initialService.searchOnSearXImages());

        MenuItem alCmItemSearchGoogleImages = new MenuItem(
                LanguageUtil.getString("key.mainview.cm.google.image.search"),
                GlyphsDude.createIcon(FontAwesomeIcon.GOOGLE)
        );
        alCmItemSearchGoogleImages.setOnAction(e -> initialService.searchOnGoogleImages());

        MenuItem alCmItemRemoveWithCover = new MenuItem(
                LanguageUtil.getString("key.mainview.cm.remove.all.with.cover"),
                GlyphsDude.createIcon(FontAwesomeIcon.TIMES_CIRCLE)
        );
        alCmItemRemoveWithCover.setOnAction(e -> initialService.clearWithCoverListEntries());

        MenuItem alCmItemRemoveSelected = new MenuItem(
                LanguageUtil.getString("key.mainview.cm.remove.all.selected"),
                GlyphsDude.createIcon(FontAwesomeIcon.TIMES_CIRCLE)
        );
        alCmItemRemoveSelected.setOnAction(e -> initialService.clearSelectedEntries());

        MenuItem alCmItemRemoveAll = new MenuItem(
                LanguageUtil.getString("key.mainview.cm.remove.all"),
                GlyphsDude.createIcon(FontAwesomeIcon.TIMES_CIRCLE)
        );
        alCmItemRemoveAll.setOnAction(e -> initialService.clearAllListEntries());

        audioListContextMenu.getItems().add(alCmItemSearchSearXImages);
        audioListContextMenu.getItems().add(alCmItemSearchGoogleImages);
        audioListContextMenu.getItems().add(new SeparatorMenuItem());
        audioListContextMenu.getItems().add(alCmItemRemoveWithCover);
        audioListContextMenu.getItems().add(alCmItemRemoveSelected);
        audioListContextMenu.getItems().add(alCmItemRemoveAll);

        lwAudioList.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (e.getButton() == MouseButton.SECONDARY && !lwAudioList.getChildren().isEmpty()) {
                audioListContextMenu.show(lwAudioList, e.getScreenX(), e.getScreenY());
            } else {
                audioListContextMenu.hide();
            }
        });
    }

    private void createIconButtons() {
        AwesomeHelper.createIconButton(
                btnFindFolder,
                FontAwesomeIcon.FOLDER_OPEN,
                MENU_ICON,
                LanguageUtil.getString("key.mainview.open.folder"),
                "24px"
        );

        AwesomeHelper.createIconButton(
                btnOpenAbout,
                FontAwesomeIcon.INFO_CIRCLE,
                MENU_ICON,
                LanguageUtil.getString("key.mainview.menu.about"),
                "24px"
        );
        AwesomeHelper.createIconButton(
                btnReportBug,
                FontAwesomeIcon.BUG,
                MENU_ICON,
                LanguageUtil.getString("key.mainview.menu.reportbug"),
                "24px"
        );
        AwesomeHelper.createIconButton(
                btnDonate,
                FontAwesomeIcon.HEART,
                MENU_ICON,
                LanguageUtil.getString("key.mainview.menu.donate"),
                "24px"
        );
        AwesomeHelper.createIconButton(
                btnOpenSettings,
                FontAwesomeIcon.GEAR,
                MENU_ICON,
                LanguageUtil.getString("key.mainview.menu.program.settings"),
                "24px"
        );
        AwesomeHelper.createIconButton(
                btnExitApp,
                FontAwesomeIcon.TIMES,
                MENU_ICON,
                LanguageUtil.getString("key.mainview.menu.program.shutdown"),
                "24px"
        );

        AwesomeHelper.createIconButton(
                btnPasteCoverFromClipBrd,
                MaterialIcon.CONTENT_PASTE,
                DEFAULT_ICON,
                LanguageUtil.getString("key.mainview.paste.from.clipbrd.cover"),
                "20px"
        );
        AwesomeHelper.createIconButton(
                btnCopyCoverToClipBrd,
                MaterialIcon.CONTENT_COPY,
                DEFAULT_ICON,
                LanguageUtil.getString("key.mainview.copy.clipbrd.cover"),
                "20px"
        );
        AwesomeHelper.createIconButton(
                btnRemoveCover,
                FontAwesomeIcon.TRASH,
                DEFAULT_ICON,
                LanguageUtil.getString("key.mainview.remove.cover"),
                "24px"
        );
        AwesomeHelper.createIconButton(
                btnOpenGoogleImageSearch,
                FontAwesomeIcon.GOOGLE,
                DEFAULT_ICON,
                LanguageUtil.getString("key.mainview.cm.google.image.search"),
                "20px"
        );

        AwesomeHelper.createTextIcon(
                txtIsMixCD,
                FontAwesomeIcon.RANDOM,
                DEFAULT_ICON_TEXT,
                LanguageUtil.getString("key.mainview.isMixCD"),
                "15px"
        );
    }

    private void registerGeneralListener() {
        lwAudioList
                .getChildren()
                .addListener(
                        (ListChangeListener<Node>) c -> {
                            if (lwAudioList.getChildren().isEmpty()) {
                                deactivateActionButton();
                                return;
                            }

                            activateActionButton(
                                    event -> ServiceLocator.get(CoverSearchService.class).search(),
                                    FontAwesomeIcon.SEARCH
                            );
                        }
                );
    }

    private void registerKeyListener() {
        SelectionService selectionService = ServiceLocator.get(SelectionService.class);
        InitialService initialService = ServiceLocator.get(InitialService.class);

        rootLayout.addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if ((System.currentTimeMillis() - lastEventExcecFireTime) <= 10) {
                return;
            }

            if (new KeyCodeCombination(KeyCode.A, KeyCombination.CONTROL_ANY).match(event)) {
                selectionService.selectAll();
                lastEventExcecFireTime = System.currentTimeMillis();
            }
            if (new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_ANY).match(event)) {
                initialService.findFolder();
                lastEventExcecFireTime = System.currentTimeMillis();
            }
            if (new KeyCodeCombination(KeyCode.DELETE).match(event)) {
                initialService.removeSelectedEntries();
                lastEventExcecFireTime = System.currentTimeMillis();
            }
            if (new KeyCodeCombination(KeyCode.HOME).match(event)) {
                selectionService.selectFirst();
                lastEventExcecFireTime = System.currentTimeMillis();
            }
            if (new KeyCodeCombination(KeyCode.UP).match(event)) {
                selectionService.selectUp();
                lastEventExcecFireTime = System.currentTimeMillis();
            }
            if (new KeyCodeCombination(KeyCode.DOWN).match(event)) {
                selectionService.selectDown();
                lastEventExcecFireTime = System.currentTimeMillis();
            }
            if (new KeyCodeCombination(KeyCode.END).match(event)) {
                selectionService.selectLast();
                lastEventExcecFireTime = System.currentTimeMillis();
            }
            if (new KeyCodeCombination(KeyCode.HOME, KeyCombination.SHIFT_ANY).match(event)) {
                selectionService.selectRangeToHome();
                scrollToLastSelected();
                lastEventExcecFireTime = System.currentTimeMillis();
            }
            if (new KeyCodeCombination(KeyCode.END, KeyCombination.SHIFT_ANY).match(event)) {
                selectionService.selectRangeToEnd();
                scrollToLastSelected();
                lastEventExcecFireTime = System.currentTimeMillis();
            }
        });
    }

    private void scrollToLastSelected() {
        AudioListEntry lastSelected = ServiceLocator.get(SelectionService.class).getLastSelected();

        if (lastSelected != null) {
            scrollToNodeInList(lastSelected);
        }
    }

    private void createBinding() {
        audioDetailsLayout.visibleProperty().bind(txtFilename.textProperty().isNotEmpty());

        btnRemoveCover.visibleProperty().bind(imgCurrentCover.imageProperty().isNotNull());
        btnCopyCoverToClipBrd.visibleProperty().bind(imgCurrentCover.imageProperty().isNotNull());
        btnOpenGoogleImageSearch.visibleProperty().bind(imgCurrentCover.imageProperty().isNotNull());

        pbStatus
                .progressProperty()
                .addListener((observable, oldValue, newValue) -> SystemUtil.setTaskbarProgress(newValue));
    }

    public void activateActionButton(EventHandler<ActionEvent> action, FontAwesomeIcon icon) {
        Platform.runLater(() -> {
            btnActionCircle.setOnAction(action);
            btnActionCircleIcon.setOnAction(action);

            AwesomeHelper.setIconButton(btnActionCircle, FontAwesomeIcon.CIRCLE, 5, "font-button", "button-circle");
            AwesomeHelper.setIconButton(btnActionCircleIcon, icon, 2.5, "font-button", "button-circle-icon");

            AwesomeHelper.createCircleAnimation(btnActionCircle, btnActionCircleIcon, 0, 1, 50, -25);
        });
    }

    public void deactivateActionButton() {
        Platform.runLater(() -> {
            btnActionCircle.setOnAction(null);
            btnActionCircleIcon.setOnAction(null);

            AwesomeHelper.createCircleAnimation(btnActionCircle, btnActionCircleIcon, 1, 0, -25, 50);
        });
    }

    private void setAudioList(Map<String, List<AudioWrapper>> audioDirectory) {
        int parentIndex = 0;
        for (Map.Entry<String, List<AudioWrapper>> entry : audioDirectory.entrySet()) {
            FolderListEntry folderEntry = new FolderListEntry(entry.getKey());
            lwAudioList.getChildren().add(folderEntry);

            if (parentIndex > 0) {
                VBox.setMargin(folderEntry, new Insets(10, 0, 0, 0));
            }

            entry.getValue().forEach(audioWrapper -> lwAudioList.getChildren().add(new AudioListEntry(audioWrapper)));

            parentIndex++;
        }
    }

    public void resetRightSide() {
        resetCurrentAudioInformation();
        Platform.runLater(() -> {
            pbStatus.setProgress(0);
            txtState.setText(null);
        });
    }

    public void resetCurrentAudioInformation() {
        Platform.runLater(() -> {
            txtFilename.setText(null);
            txtArtist.setText(null);
            txtTitle.setText(null);
            txtAlbum.setText(null);
            imgCurrentCover.setImage(null);
            txtCurrentAudioCoverRes.setText(null);
            txtIsMixCD.setVisible(false);
        });
    }

    public void setState(String msg) {
        Platform.runLater(() -> txtState.setText(msg));
    }

    public void showBusyIndicator(String title, ActionListener cancelListener) {
        Platform.runLater(() -> {
            VBox layout = new VBox(25);
            progressScanIndicator = new ProgressIndicator();
            progressScanIndicator.setStyle("-fx-accent: #FF6F00; -fx-progress-color: #FF6F00;");

            txtAudioToLoad = new TextFlow();
            txtAudioToLoad.setTextAlignment(TextAlignment.CENTER);

            layout.getChildren().add(txtAudioToLoad);
            layout.getChildren().add(progressScanIndicator);

            VBox.setVgrow(progressScanIndicator, Priority.ALWAYS);
            VBox.setMargin(progressScanIndicator, new Insets(0, 30, 0, 30));
            VBox.setVgrow(txtAudioToLoad, Priority.NEVER);

            layout.setMinWidth(300);
            layout.setAlignment(Pos.TOP_CENTER);
            layout.setPadding(new Insets(10, 0, 10, 0));

            ServiceLocator.get(LightBoxService.class).showDialog(
                    title,
                    layout,
                    cancelListener,
                    null,
                    false,
                    cancelListener == null
            );
        });
    }

    public void hideBusyIndicator() {
        Platform.runLater(() -> {
            ServiceLocator.get(LightBoxService.class).hideDialog();
            progressScanIndicator = null;
        });
    }

    public void setTotalAudioCountToLoad(int totalAudioCount) {
        Platform.runLater(() -> {
            if (progressScanIndicator == null) {
                return;
            }

            this.totalAudioCount = totalAudioCount;
            this.currentAudioCount = new AtomicInteger(0);

            Text fixPreText = new Text(LanguageUtil.getString("key.mainview.txtTotalMp3s") + " ");
            txtUpcountIndicatorValue = new Text();

            txtAudioToLoad.getChildren().add(fixPreText);
            txtAudioToLoad.getChildren().add(txtUpcountIndicatorValue);
            txtAudioToLoad.getChildren().add(new Text(String.format(" / %s", totalAudioCount)));
        });
    }

    public void countIndicatorUp() {
        Platform.runLater(() -> {
            if (progressScanIndicator == null || txtUpcountIndicatorValue == null) {
                return;
            }

            currentAudioCount.incrementAndGet();
            double value = currentAudioCount.doubleValue() / totalAudioCount;

            txtUpcountIndicatorValue.setText(currentAudioCount.toString());
            progressScanIndicator.setProgress(value);
        });
    }

    public void highlightInList(Integer audioWrapperId) {
        Platform.runLater(() -> {
            int i = lwAudioList.getChildren().indexOf(getAudioListEntry(audioWrapperId));
            lwAudioList.getChildren().get(i).getStyleClass().add(AUDIO_LINE_SELECTED);
        });
    }

    public void highlightInList(AudioListEntry currentAudio) {
        Platform.runLater(() -> {
            int i = lwAudioList.getChildren().indexOf(currentAudio);
            lwAudioList.getChildren().get(i).getStyleClass().add(AUDIO_LINE_SELECTED);
        });
    }

    public void highlightRangeInList(AudioListEntry from, AudioListEntry to) {
        Platform.runLater(() -> {
            ArrayList<AudioListEntry> selectedEntries = new ArrayList<>();
            AudioWrapper fromWrapper = from.getWrapper();
            AudioWrapper toWrapper = to.getWrapper();

            boolean isInRange = false;
            List<AudioListEntry> children = lwAudioList
                    .getChildren()
                    .stream()
                    .filter(AudioListEntry.class::isInstance)
                    .map(AudioListEntry.class::cast)
                    .toList();

            int fromIndex = children.indexOf(from);
            int toIndex = children.indexOf(to);

            if (fromIndex > toIndex) {
                fromWrapper = to.getWrapper();
                toWrapper = from.getWrapper();
            }

            for (AudioListEntry entry : children) {
                if (entry.getWrapper().getId().equals(fromWrapper.getId())) {
                    isInRange = true;
                }
                if (entry.getWrapper().getId().equals(toWrapper.getId())) {
                    entry.getStyleClass().add(AUDIO_LINE_SELECTED);
                    selectedEntries.add(entry);
                    isInRange = false;
                }

                if (isInRange) {
                    entry.getStyleClass().add(AUDIO_LINE_SELECTED);
                    selectedEntries.add(entry);
                }
            }

            ServiceLocator.get(SelectionService.class).setSelectedEntries(selectedEntries);
        });
    }

    public void unhighlightInList(Integer audioWrapperId) {
        Platform.runLater(() ->
                lwAudioList
                        .getChildren()
                        .stream()
                        .filter(AudioListEntry.class::isInstance)
                        .map(AudioListEntry.class::cast)
                        .filter(entry -> entry.getWrapper().getId().equals(audioWrapperId))
                        .forEach(entry -> entry.getStyleClass().removeAll(AUDIO_LINE_SELECTED))
        );
    }

    public void unhighlightAll() {
        Platform.runLater(() ->
                lwAudioList
                        .getChildren()
                        .stream()
                        .map(x -> ((HBox) x))
                        .forEach(x -> x.getStyleClass().removeAll(AUDIO_LINE_SELECTED))
        );
    }

    public void highlightAll() {
        Platform.runLater(() ->
                lwAudioList
                        .getChildren()
                        .stream()
                        .filter(AudioListEntry.class::isInstance)
                        .map(x -> ((AudioListEntry) x))
                        .forEach(x -> x.getStyleClass().add(AUDIO_LINE_SELECTED))
        );
    }

    public void setNewCoverToListItem(AudioWrapper currentAudio, ImageFile newCover) {
        Platform.runLater(() -> {
            AudioListEntry listEntry = getAudioListEntry(currentAudio.getId());

            if (listEntry == null) {
                return;
            }

            try {
                Image previewImage = new Image(new FileInputStream(newCover.filePath()), 36, 36, true, false);
                createSingleLineAnimation(previewImage, listEntry);
                listEntry.getIconView().setIcon(FontAwesomeIcon.CHECK);
                listEntry.getWrapper().setHasCover(true);
            } catch (FileNotFoundException e) {
                log.error(e.getMessage(), e);
            }
        });
    }

    public void setNewCoverToListItem(AudioWrapper currentAudio, BufferedImage newCover) {
        Platform.runLater(() -> {
            AudioListEntry listEntry = getAudioListEntry(currentAudio.getId());

            if (listEntry == null) {
                return;
            }

            Image previewImage = SwingFXUtils.toFXImage(ImageUtil.resize(newCover, 36, 36), null);
            createSingleLineAnimation(previewImage, listEntry);
            listEntry.getIconView().setIcon(FontAwesomeIcon.CHECK);
            listEntry.getWrapper().setHasCover(true);
        });
    }

    public void createSingleLineAnimation(Image coverImage, AudioListEntry entry) {
        Platform.runLater(() -> {
            ImageView imageView = entry.getImageView();
            if (imageView == null) {
                return;
            }
            createFadeAnimation(imageView);
            imageView.setImage(coverImage);
        });
    }

    public AudioListEntry getAudioListEntry(Integer audioWrapperId) {
        return lwAudioList
                .getChildren()
                .stream()
                .filter(AudioListEntry.class::isInstance)
                .map(x -> (AudioListEntry) x)
                .filter(x -> x.getWrapper().getId().equals(audioWrapperId))
                .findFirst()
                .orElse(null);
    }

    public void scrollToNodeInList(AudioWrapper currentAudio) {
        Platform.runLater(() -> {
            scrollTo(getAudioListEntry(currentAudio.getId()));
            ServiceLocator.get(SelectionService.class).addSelection(getAudioListEntry(currentAudio.getId()));
        });
    }

    public void scrollToNodeInList(AudioListEntry entry) {
        Platform.runLater(() -> scrollTo(entry));
    }

    private void scrollTo(AudioListEntry entry) {
        try {
            double height = spAudioList.getContent().getBoundsInLocal().getHeight();
            double y = entry.getBoundsInParent().getMaxY();

            spAudioList.setVvalue(y / height);
            entry.requestFocus();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void onMultipleSelectionStared() {
        Platform.runLater(() -> {
            ViewManager viewManager = ViewManager.getInstance();
            Parent layoutFromView = viewManager.createLayoutFromView(Views.MULTI_SELECTION_LAYOUT_VIEW);
            MultiselectionLayoutViewController viewController = viewManager.getViewController(
                    Views.MULTI_SELECTION_LAYOUT_VIEW
            );
            viewController.setClearCoverText(LanguageUtil.getString("remove.all.cover.from.selected.audio.files"));

            multiSelectionLayout.getChildren().clear();
            multiSelectionLayout.getChildren().add(layoutFromView);
            multiSelectionLayout.setVisible(true);

            AnimationHelper.slide(multiSelectionLayout, 0, 1, 150, 5);
        });
    }

    @Override
    public void onMultipleSelectionEnded() {
        Platform.runLater(() -> {
            AnimationHelper.slide(multiSelectionLayout, 1, 0, 5, 150);
        });
    }

    @Override
    public void onParentListEntryDeleted(String key) {
        List<FolderListEntry> entriesToRemove = lwAudioList
                .getChildren()
                .stream()
                .filter(FolderListEntry.class::isInstance)
                .map(x -> ((FolderListEntry) x))
                .filter(x -> x.getPath().equalsIgnoreCase(key))
                .toList();

        lwAudioList.getChildren().removeAll(entriesToRemove);
    }

    public void setAudioNodeBusy(boolean isBusy, Integer audioWrapperId) {
        getAudioListEntry(audioWrapperId).setBusy(isBusy);
    }

    public void setEntryToProcessingState(AudioWrapper audioWrapper) {
        Platform.runLater(() -> {
            showAudioInfo(audioWrapper, false);
            highlightInList(audioWrapper.getId());
            setAudioNodeBusy(true, audioWrapper.getId());
        });
    }

    public void setEntryToFinishedState(Integer audioWrapperId) {
        Platform.runLater(() -> {
            unhighlightInList(audioWrapperId);
            setAudioNodeBusy(false, audioWrapperId);
            increaseProgress();
        });
    }

    public void setBusyIndicatorStatusText(String text) {
        Platform.runLater(() -> {
            txtAudioToLoad.getChildren().clear();
            txtAudioToLoad.getChildren().add(new Text(text));
        });
    }
}
