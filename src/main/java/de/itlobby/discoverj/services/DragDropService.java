package de.itlobby.discoverj.services;

import de.itlobby.discoverj.components.AudioListEntry;
import de.itlobby.discoverj.framework.ServiceLocator;
import de.itlobby.discoverj.framework.ViewManager;
import de.itlobby.discoverj.framework.Views;
import de.itlobby.discoverj.listeners.ActionListener;
import de.itlobby.discoverj.models.AudioWrapper;
import de.itlobby.discoverj.models.SimpleAudioWrapper;
import de.itlobby.discoverj.util.AudioUtil;
import de.itlobby.discoverj.util.GlyphsDude;
import de.itlobby.discoverj.util.ImageUtil;
import de.itlobby.discoverj.util.LanguageUtil;
import de.itlobby.discoverj.util.SystemUtil;
import de.itlobby.discoverj.viewcontroller.OpenFileViewController;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.animation.ScaleTransition;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.text.Text;
import javafx.util.Duration;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

public class DragDropService implements Service {
    private static final Logger LOG = LogManager.getLogger(DragDropService.class);

    private static void createScaleAnimation(Node circle) {
        ScaleTransition st = new ScaleTransition(Duration.millis(500), circle);
        st.setByX(0.1);
        st.setByY(0.1);
        st.setCycleCount(999);
        st.setAutoReverse(true);
        st.play();
    }

    static Text createIcon(FontAwesomeIcon music) {
        Text iconTxt = GlyphsDude.createIcon(music, "82px");
        iconTxt.getStyleClass().add("default-icon");
        return iconTxt;
    }

    private void insertDroppedCover(String url) {
        try {
            URL imgUrl = new URL(url);
            File imgFile = SystemUtil.getTempFileFromUrl(imgUrl);
            FileUtils.copyURLToFile(imgUrl, imgFile);
            insertDroppedCover(imgFile);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    void insertDroppedCover(File imgFile) {
        List<AudioListEntry> selectedEntries = ServiceLocator.get(SelectionService.class).getSelectedEntries();
        if (selectedEntries.isEmpty()) {
            return;
        }

        getMainViewController().showBusyIndicator(LanguageUtil.getString("add.images.to.audiofiles"));

        new Thread(() ->
                addCoverToEntries(
                        imgFile,
                        selectedEntries,
                        () -> getMainViewController().hideBusyIndicator()
                ))
                .start();
    }

    private void addCoverToEntries(File imgFile, List<AudioListEntry> selectedEntries, ActionListener threadFinishedListener) {
        try {
            Optional<BufferedImage> img = ImageUtil.readRGBImage(imgFile);

            if (img.isEmpty()) {
                return;
            }

            getMainViewController().setTotalAudioCountToLoad(selectedEntries.size());

            for (AudioListEntry selectedEntry : selectedEntries) {
                getMainViewController().countIndicatorUp();
                SimpleAudioWrapper audioWrapper = selectedEntry.getSimpleAudioWrapper();
                AudioFile audioFile = AudioFileIO.read(new File(audioWrapper.getPath()));

                List<SimpleAudioWrapper> audioList = ServiceLocator.get(DataService.class).getAudioList();

                audioList.stream()
                        .filter(x -> x.getId().equals(audioWrapper.getId()))
                        .forEach(x -> AudioUtil.saveCoverToAudioFile(audioFile, img.get()));

                getMainViewController().lwAudioList.getChildren()
                        .stream()
                        .filter(AudioListEntry.class::isInstance)
                        .map(AudioListEntry.class::cast)
                        .filter(audioEntry -> audioEntry.getSimpleAudioWrapper().getId().equals(audioWrapper.getId()))
                        .forEach(wrapper -> wrapper.replaceCover(img.get()));
            }

            getMainViewController().showAudioInfo(new AudioWrapper(selectedEntries.get(0).getSimpleAudioWrapper()));
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        } finally {
            threadFinishedListener.onAction();
        }
    }

    void initDragAndDrop() {
        getMainViewController().rootLayout.setOnDragOver(event -> {
                    Dragboard db = event.getDragboard();
                    if (db.hasFiles() || db.hasUrl()) {
                        event.acceptTransferModes(TransferMode.ANY);
                    }
                    event.consume();
                }
        );

        getMainViewController().rootLayout.setOnDragEntered(event -> {
                    Dragboard db = event.getDragboard();
                    List<File> files = db.getFiles();

                    int audioCount = (int) files.stream().filter(AudioUtil::isAudioFile).count();
                    int dirCount = (int) files.stream().filter(File::isDirectory).count();
                    int imgCount = (int) files.stream().filter(SystemUtil::isImageFile).count();
                    //TODO: add html google parsing
                    boolean isLink = db.hasUrl() && db.getUrl().startsWith("http") && SystemUtil.isImage(db.getUrl());

                    if (audioCount > 0 || dirCount > 0) {
                        imgCount = 0;
                    }

                    createDragOverAnimation(audioCount, imgCount, dirCount, isLink);
                    event.consume();
                }
        );

        getMainViewController().rootLayout.setOnDragExited(event -> ServiceLocator.get(LightBoxService.class).hideDialog());
        getMainViewController().rootLayout.setOnDragDropped(this::dragDropped);
    }

    private void dragDropped(DragEvent event) {
        Dragboard db = event.getDragboard();
        boolean success = false;

        List<File> files = db.getFiles();

        int audioCount = (int) files.stream().filter(AudioUtil::isAudioFile).count();
        int dirCount = (int) files.stream().filter(File::isDirectory).count();
        int imgCount = (int) files.stream().filter(SystemUtil::isImageFile).count();
        boolean isValidLink = db.hasUrl() && db.getUrl().startsWith("http") && SystemUtil.isImage(db.getUrl());

        if (db.hasFiles() && (dirCount > 0 || audioCount > 0)) {
            File[] fileArray = new File[files.size()];
            ServiceLocator.get(InitialService.class).beginScanFiles(files.toArray(fileArray));
            success = true;
        } else if (isValidLink) {
            insertDroppedCover(db.getUrl());
            success = true;
        } else if (db.hasFiles() && imgCount > 0) {
            insertDroppedCover(files.get(0));
            success = true;
        }

        ServiceLocator.get(LightBoxService.class).hideDialog();
        event.setDropCompleted(success);
        event.consume();
    }

    private void createDragOverAnimation(int audioCount, int imgCount, int folderCount, boolean isLink) {
        Parent root = ViewManager.getInstance().createLayoutFromView(Views.DROP_FILE_VIEW);
        OpenFileViewController vc = ViewManager.getInstance().getViewController(Views.DROP_FILE_VIEW);

        if (audioCount == 1) {
            vc.lblIntroduction.setText(String.format(LanguageUtil.getString("drop.mp3s.here"), audioCount));
        } else if (audioCount > 1) {
            vc.lblIntroduction.setText(String.format(LanguageUtil.getString("drop.s.mp3s.here"), audioCount));
        } else if (folderCount > 0) {
            vc.lblIntroduction.setText(String.format(LanguageUtil.getString("drop.dirs.here"), audioCount));
        } else if (imgCount > 0) {
            vc.lblIntroduction.setText(String.format(LanguageUtil.getString("drop.img.here"), audioCount));
        } else if (isLink) {
            vc.lblIntroduction.setText(String.format(LanguageUtil.getString("drop.url.here"), audioCount));
        } else {
            vc.lblIntroduction.setText(String.format(LanguageUtil.getString("drop.invalid.file"), audioCount));
        }

        vc.lblIntroduction.getStyleClass().add("drop-zone-text");

        Text icon;

        if (imgCount > 0) {
            icon = createIcon(FontAwesomeIcon.IMAGE);
        } else if (folderCount > 0) {
            icon = createIcon(FontAwesomeIcon.FOLDER_OPEN);
        } else if (audioCount > 0) {
            icon = createIcon(FontAwesomeIcon.MUSIC);
        } else if (isLink) {
            icon = createIcon(FontAwesomeIcon.LINK);
        } else {
            icon = createIcon(FontAwesomeIcon.BAN);
        }

        icon.getStyleClass().add("button-circle-icon");
        vc.textLayout.getChildren().add(icon);
        createScaleAnimation(icon);
        createScaleAnimation(vc.circleBorder);

        ServiceLocator.get(LightBoxService.class).showDialog(LanguageUtil.getString("drag.drop.title"), root, null, null, false, true);
    }

}
