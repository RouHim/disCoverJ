package de.itlobby.discoverj.services;

import de.itlobby.discoverj.mixcd.MixCd;
import de.itlobby.discoverj.models.AudioWrapper;
import de.itlobby.discoverj.models.ScanResultData;
import de.itlobby.discoverj.settings.AppConfig;
import de.itlobby.discoverj.settings.Settings;
import de.itlobby.discoverj.ui.components.AudioListEntry;
import de.itlobby.discoverj.ui.core.ServiceLocator;
import de.itlobby.discoverj.ui.core.ViewManager;
import de.itlobby.discoverj.ui.core.Views;
import de.itlobby.discoverj.ui.viewcontroller.CoverDetailViewController;
import de.itlobby.discoverj.ui.viewcontroller.MainViewController;
import de.itlobby.discoverj.ui.viewcontroller.OpenFileViewController;
import de.itlobby.discoverj.ui.viewtask.PreCountViewTask;
import de.itlobby.discoverj.ui.viewtask.ScanFileViewTask;
import de.itlobby.discoverj.util.AsyncPipeline;
import de.itlobby.discoverj.util.AudioUtil;
import de.itlobby.discoverj.util.ImageCache;
import de.itlobby.discoverj.util.ImageClipboardUtil;
import de.itlobby.discoverj.util.LanguageUtil;
import de.itlobby.discoverj.util.SearXUtil;
import de.itlobby.discoverj.util.StringUtil;
import de.itlobby.discoverj.util.SystemUtil;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jaudiotagger.audio.AudioFile;

import java.io.File;
import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;

import static de.itlobby.discoverj.util.AudioUtil.removeCover;

public class InitialService implements Service {
    private static final Logger log = LogManager.getLogger(InitialService.class);
    private PreCountViewTask preCountTask;
    private ScanFileViewTask scanFileTask;
    private boolean interruptProgress = false;

    public void setInterruptProgress(boolean interruptProgress) {

        if (interruptProgress && preCountTask != null) {
            preCountTask.cancel();
        }

        if (interruptProgress && scanFileTask != null) {
            scanFileTask.cancel();
        }

        this.interruptProgress = interruptProgress;
    }

    public void openInitialOpenDialog() {
        Parent root = ViewManager.getInstance().createLayoutFromView(Views.DROP_FILE_VIEW);
        OpenFileViewController viewController = ViewManager.getInstance().getViewController(Views.DROP_FILE_VIEW);

        viewController.lblIntroduction.setText(LanguageUtil.getString("select.mp3s.here"));
        viewController.lblIntroduction.getStyleClass().add("drop-zone-text");

        Text icon = DragDropService.createIcon(FontAwesomeIcon.MUSIC);
        icon.getStyleClass().add("button-circle-icon");
        viewController.textLayout.getChildren().add(icon);
        viewController.rootLayout.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> findFolder());

        LightBoxService lightBoxService = ServiceLocator.get(LightBoxService.class);
        lightBoxService.showDialog(LanguageUtil.getString("select.music"), root, null, null, false, true);

        viewController.btnExitDialog.setOnAction(event -> lightBoxService.hideDialog());
    }

    public void findFolder() {
        AppConfig config = Settings.getInstance().getConfig();

        File lastPath = new File(config.getLastFolderPath() == null ? "" : config.getLastFolderPath());

        File folder = SystemUtil.getFileFromFolderChooser(lastPath);

        if (folder != null) {
            String absolutePath = folder.getAbsolutePath();
            finishFindFolder(absolutePath);

            config.setLastFolderPath(absolutePath);
            Settings.getInstance().saveConfig(config);
        }
    }

    private void finishFindFolder(String absolutePath) {
        ServiceLocator.get(LightBoxService.class).hideDialog();
        initMusic(absolutePath);
    }

    private void initMusic(String path) {
        File musicFolder = new File(path);

        if (!musicFolder.exists()) {
            ServiceLocator.get(LightBoxService.class).showTextDialog(
                    LanguageUtil.getString("InitialController.warning"),
                    LanguageUtil.getString("InitialController.folderDoesnotExists")
            );
            return;
        }

        if (!musicFolder.isDirectory()) {
            ServiceLocator.get(LightBoxService.class).showTextDialog(
                    LanguageUtil.getString("InitialController.warning"),
                    LanguageUtil.getString("InitialController.invalidFolder")
            );
            return;
        }

        beginScanFiles(musicFolder);
    }

    public void beginScanFiles(File... musicObjects) {
        DataHolder.getInstance().clear();
        MixCd.clearCache();
        ImageCache.getInstance().clear();

        MainViewController viewController = getMainViewController();
        viewController.resetRightSide();
        viewController.showBusyIndicator(LanguageUtil.getString("InitialController.loadingMp3List"), null);
        viewController.setState(LanguageUtil.getString("InitialController.loadingMp3List"));

        //Filter and count audio files
        try {
            preCountTask = new PreCountViewTask(musicObjects);
            preCountTask.setFinishedListener(this::scanFiles);

            Thread.ofVirtual()
                    .uncaughtExceptionHandler(ServiceLocator.get(ExceptionService.class))
                    .start(preCountTask);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void scanFiles(List<String> fileList) {
        if (interruptProgress) {
            return;
        }

        int fileCount = fileList.size();
        getMainViewController().setTotalAudioCountToLoad(fileCount);
        log.info("To scan: {}", fileCount);

        try {
            scanFileTask = new ScanFileViewTask(fileList, getMainViewController());
            scanFileTask.setFinishedListener(this::scanFinished);

            Thread.ofVirtual()
                    .uncaughtExceptionHandler(ServiceLocator.get(ExceptionService.class))
                    .start(scanFileTask);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void scanFinished(ScanResultData scanResultData) {
        DataHolder.getInstance().setFromScanResult(scanResultData);

        getMainViewController().showScanResult(
                scanResultData.getWithCoverCount(),
                scanResultData.getAudioFilesCount(),
                scanResultData.getAudioMap()
        );

        ServiceLocator.get(SelectionService.class).clearAll();
    }

    public void showCurrentCoverDetailed() {
        AudioListEntry audioListEntry = ServiceLocator.get(SelectionService.class).getLastSelected();

        if (audioListEntry == null) {
            return;
        }

        AudioWrapper audioWrapper = audioListEntry.getWrapper();
        String title = MessageFormat.format(LanguageUtil.getString("cover.von.0"), audioWrapper.getDisplayValue());

        ServiceLocator.get(LightBoxService.class).showDialog(
                title,
                createDetailCoverLayout(),
                null,
                null,
                true,
                false
        );

        CoverDetailViewController vc = ViewManager.getInstance().getViewController(Views.COVER_DETAIL_VIEW);

        audioWrapper.loadImage().ifPresentOrElse(coverImage -> {
            vc.imgCover.setImage(coverImage);
            vc.createCoverInfo(coverImage);
            vc.fitImageSizeImage(getMainViewController().lightBoxLayout);
        }, vc::clearCoverInfo);
    }

    private Parent createDetailCoverLayout() {
        return ViewManager.getInstance().createLayoutFromView(Views.COVER_DETAIL_VIEW);
    }

    public void selectLine(Node clickedEntry, boolean controlDown, boolean shiftDown) {
        SelectionService selectionService = ServiceLocator.get(SelectionService.class);

        if (clickedEntry != null) {
            if (controlDown && !shiftDown) {
                selectionService.addSelection(clickedEntry);
            } else if (!controlDown && shiftDown) {
                selectionService.rangeSelection(clickedEntry);
            } else if (!controlDown) {
                selectionService.selectNode(clickedEntry);
            }
        }
    }

    public void removeLastSelectedCover() {
        AudioListEntry audioListEntry = ServiceLocator.get(SelectionService.class).getLastSelected();
        AudioWrapper audioWrapper = audioListEntry.getWrapper();

        if (!audioWrapper.hasCover()) {
            return;
        }

        removeCover(audioWrapper);

        DataHolder.getInstance().updateResultEntry(audioWrapper);

        getMainViewController().lwAudioList.getChildren()
                .stream()
                .filter(AudioListEntry.class::isInstance)
                .map(AudioListEntry.class::cast)
                .filter(x -> x.getWrapper().getId().equals(audioWrapper.getId()))
                .forEach(AudioListEntry::removeCover);

        selectLine(audioListEntry, false, false);
    }

    public void removeAllSelectedCover() {
        DataHolder dataHolder = DataHolder.getInstance();
        SelectionService selectionService = ServiceLocator.get(SelectionService.class);
        List<AudioListEntry> selectedEntries = selectionService.getSelectedEntries();

        AsyncPipeline.run(
                        () -> {
                            getMainViewController().showBusyIndicator(LanguageUtil.getString("key.mainview.cm.remove.all.selected"), null);
                            getMainViewController().setTotalAudioCountToLoad(selectedEntries.size());
                        }
                )
                .andThen(
                        () -> selectedEntries.stream()
                                .map(AudioListEntry::getWrapper)
                                .filter(AudioWrapper::hasCover)
                                .forEach((audioEntry) -> {
                                    getMainViewController().countIndicatorUp();
                                    AudioUtil.removeCover(audioEntry);
                                }))
                .andThen(
                        () -> selectedEntries.stream()
                                .map(AudioListEntry::getWrapper)
                                .forEach(dataHolder::updateResultEntry))
                .andThen(
                        () -> Platform.runLater(() -> {
                            getMainViewController().lwAudioList.getChildren()
                                    .stream()
                                    .filter(AudioListEntry.class::isInstance)
                                    .map(AudioListEntry.class::cast)
                                    .filter(x -> selectedEntries.stream().anyMatch(y -> y.getWrapper().getId().equals(x.getWrapper().getId())))
                                    .forEach(AudioListEntry::removeCover);
                        })
                )
                // hide busy indicator
                .andThen(() -> getMainViewController().hideBusyIndicator())
                .begin();

        selectionService.setSelectedEntries(selectedEntries);
    }

    public void copyCoverToClipBrd() {
        AudioListEntry audioListEntry = ServiceLocator.get(SelectionService.class).getLastSelected();
        AudioWrapper audioWrapper = audioListEntry.getWrapper();

        if (audioWrapper.hasCover()) {
            audioWrapper.loadImage().ifPresent(image ->
                    Thread.ofVirtual().start(() -> new ImageClipboardUtil().setImage(image))
            );
        }
    }

    public void pasteCoverFromClipBrd() {
        Thread.ofVirtual().start(() -> {
            AudioListEntry audioListEntry = ServiceLocator.get(SelectionService.class).getLastSelected();
            AudioWrapper audioWrapper = audioListEntry.getWrapper();
            audioWrapper.setHasCover(true);

            Optional<AudioFile> audioFile = AudioUtil.readAudioFile(audioWrapper.getFilePath());
            if (audioFile.isEmpty()) {
                return;
            }

            ImageClipboardUtil.getImage().ifPresent(coverImage -> {
                AudioUtil.saveCoverToAudioFile(audioFile.get(), coverImage);
                getMainViewController().setNewCoverToListItem(audioWrapper, coverImage);
                getMainViewController().showAudioInfo(audioWrapper, true);
            });
        });
    }

    public void removeSelectedEntries() {
        List<AudioListEntry> selectedEntries = ServiceLocator.get(SelectionService.class).getSelectedEntries();

        if (!selectedEntries.isEmpty()) {
            DataHolder.getInstance().removeResultEntries(
                    selectedEntries
                            .stream()
                            .map(AudioListEntry::getWrapper)
                            .toList());

            getMainViewController().lwAudioList.getChildren().removeAll(selectedEntries);
            ServiceLocator.get(SelectionService.class).clearAll();
        }
    }

    public void clearAllListEntries() {
        DataHolder.getInstance().clear();
        getMainViewController().lwAudioList.getChildren().clear();
        ServiceLocator.get(SelectionService.class).clearAll();
    }

    public void clearWithCoverListEntries() {
        DataHolder dataHolder = DataHolder.getInstance();
        dataHolder.removeItemsWithCover();

        getMainViewController().showScanResult(
                dataHolder.getWithCoverCount(),
                dataHolder.getAudioFilesCount(),
                dataHolder.getAudioMap()
        );

        ServiceLocator.get(SelectionService.class).clearAll();
    }

    public void clearSelectedEntries() {
        List<Integer> selectedIds = ServiceLocator.get(SelectionService.class).getSelectedEntries()
                .stream()
                .map(x -> x.getWrapper().getId())
                .toList();

        DataHolder dataHolder = DataHolder.getInstance();
        dataHolder.removeItemsById(selectedIds);

        getMainViewController().showScanResult(
                dataHolder.getWithCoverCount(),
                dataHolder.getAudioFilesCount(),
                dataHolder.getAudioMap()
        );

        ServiceLocator.get(SelectionService.class).clearAll();
    }

    public void searchOnSearXImages() {
        AudioListEntry lastSelected = ServiceLocator.get(SelectionService.class).getLastSelected();

        if (lastSelected != null) {
            String googleSearchPattern = Settings.getInstance().getConfig().getGoogleSearchPattern();
            String rawQuery = SearchQueryUtil.createSearchQueryFromPattern(lastSelected.getWrapper(), googleSearchPattern);
            String query = StringUtil.encodeRfc3986(rawQuery);

            Thread.ofPlatform().start(() -> {
                String url = SearXUtil.getInstances().get(0);
                SystemUtil.browseUrl(String.format(url + "/search?q=%s&category_images", query));
            });
            openSelectImageDialog();
        }
    }

    public void searchOnGoogleImages() {
        AudioListEntry lastSelected = ServiceLocator.get(SelectionService.class).getLastSelected();

        if (lastSelected != null) {
            String googleSearchPattern = Settings.getInstance().getConfig().getGoogleSearchPattern();
            String rawQuery = SearchQueryUtil.createSearchQueryFromPattern(lastSelected.getWrapper(), googleSearchPattern);
            String query = StringUtil.encodeRfc3986(rawQuery);

            SystemUtil.browseUrl(String.format("https://www.google.com/search?q=%s&tbm=isch", query));
            openSelectImageDialog();
        }
    }

    private void openSelectImageDialog() {
        Parent root = ViewManager.getInstance().createLayoutFromView(Views.DROP_FILE_VIEW);
        OpenFileViewController viewController = ViewManager.getInstance().getViewController(Views.DROP_FILE_VIEW);

        viewController.lblIntroduction.setText(LanguageUtil.getString("select.image.here"));
        viewController.lblIntroduction.getStyleClass().add("drop-zone-text");

        Text icon = DragDropService.createIcon(FontAwesomeIcon.IMAGE);
        icon.getStyleClass().add("button-circle-icon");
        viewController.textLayout.getChildren().add(icon);
        viewController.rootLayout.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> findImage());

        LightBoxService lightBoxService = ServiceLocator.get(LightBoxService.class);
        lightBoxService.showDialog(LanguageUtil.getString("select.cover"), root, null, null, false, true);

        viewController.btnExitDialog.setOnAction(event -> lightBoxService.hideDialog());
    }

    private void findImage() {
        File imageFile = SystemUtil.getImageFileFromFileChooser(LanguageUtil.getString("select.cover"));

        if (imageFile != null) {
            ServiceLocator.get(DragDropService.class).insertDroppedCover(imageFile);
        }
    }
}