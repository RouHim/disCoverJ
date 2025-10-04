package de.itlobby.discoverj.ui.viewtask;

import de.itlobby.discoverj.models.AudioWrapper;
import de.itlobby.discoverj.models.ScanResultData;
import de.itlobby.discoverj.settings.Settings;
import de.itlobby.discoverj.ui.components.AudioListEntry;
import de.itlobby.discoverj.ui.viewcontroller.MainViewController;
import de.itlobby.discoverj.util.AudioUtil;
import de.itlobby.discoverj.util.SystemUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javafx.scene.image.Image;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jaudiotagger.audio.AudioFile;

public class ScanFileViewTask extends ViewTask<ScanResultData> {

    private static final Logger log = LogManager.getLogger(ScanFileViewTask.class);
    private final List<String> filesToLoad;
    private final MainViewController mainViewController;
    private final int totalAudioCountToLoad;
    private List<AudioWrapper> audioWrapperList;
    private Integer idCount;
    private int withCover;

    public ScanFileViewTask(List<String> filesToLoad, MainViewController mainViewController) {
        this.filesToLoad = filesToLoad;
        this.mainViewController = mainViewController;
        totalAudioCountToLoad = filesToLoad.size();
        idCount = 0;

        audioWrapperList = new ArrayList<>();
    }

    @Override
    public void work() {
        if (totalAudioCountToLoad <= 0) {
            log.info("No files to load");
            setResult(new ScanResultData(new LinkedHashMap<>(), 0, 0));
            return;
        }

        // Load audio information of all files
        filesToLoad.forEach(this::scanFile);

        // Sort audio list by file path
        try {
            Collections.sort(audioWrapperList);
        } catch (Exception e) {
            log.error("ERROR while trying to sortByKey mp3 list", e);
            audioWrapperList.sort(Comparator.comparing(AudioWrapper::getFilePath));
        }

        // Add audio list to audioData list
        int audioFilesCount = audioWrapperList.size();

        // Group audio list by parent file path and sort by folder name
        Map<String, List<AudioWrapper>> audioData = audioWrapperList
            .stream()
            .collect(Collectors.groupingBy(AudioWrapper::getParentFilePath))
            .entrySet()
            .stream()
            .sorted(Map.Entry.comparingByKey())
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (oldValue, newValue) -> oldValue,
                    LinkedHashMap::new
                )
            );

        // If there are less than 500 audio files, load cover images async
        if (totalAudioCountToLoad <= 500) {
            Settings.getInstance().setCoverLoadingDisabled(false);
            // Create map with id and file path and handover to the lazy load thread
            var idPathMap = audioWrapperList
                .stream()
                .collect(Collectors.toMap(AudioWrapper::getId, AudioWrapper::getFilePath));
            Thread.ofVirtual().start(() -> lazyLoadCoverImages(idPathMap));
        } else {
            Settings.getInstance().setCoverLoadingDisabled(true);
        }

        // Unload audioWrapperList
        audioWrapperList = null;

        // Handover result data to tne next task
        setResult(new ScanResultData(audioData, audioFilesCount, withCover));
    }

    private void lazyLoadCoverImages(Map<Integer, String> audioList) {
        while (mainViewController.lwAudioList.getChildren().size() < audioList.size() && !isCancelled()) {
            SystemUtil.threadSleep(100);
        }

        for (Map.Entry<Integer, String> audioEntry : audioList.entrySet()) {
            if (isCancelled()) {
                return;
            }

            Optional<Image> coverImage = AudioUtil.getCover(audioEntry.getValue(), 36, 36);

            if (coverImage.isEmpty()) {
                continue;
            }

            mainViewController.lwAudioList
                .getChildren()
                .stream()
                .filter(AudioListEntry.class::isInstance)
                .map(AudioListEntry.class::cast)
                .filter(x -> x.getWrapper().getId().equals(audioEntry.getKey()))
                .findFirst()
                .ifPresent(audioListEntry ->
                    mainViewController.createSingleLineAnimation(coverImage.get(), audioListEntry)
                );

            SystemUtil.threadSleep(50);
        }
    }

    private void scanFile(String rootFile) {
        File file = new File(rootFile);

        if (isCancelled()) {
            return;
        }

        if (file.isFile()) {
            validateAndAddFile(file);
            return;
        }

        File[] filesFromDir = file.listFiles();
        if (filesFromDir == null) {
            return;
        }

        for (File fileObj : filesFromDir) {
            if (isCancelled()) {
                return;
            }

            if (fileObj.isDirectory()) {
                scanFile(rootFile);
            } else {
                validateAndAddFile(fileObj);
            }
        }
    }

    private void validateAndAddFile(File file) {
        if (!AudioUtil.isAudioFile(file)) {
            return;
        }

        try {
            addAudioToList(file.getAbsolutePath());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void addAudioToList(String filePath) {
        var maybeAudioFile = AudioUtil.readAudioFile(filePath);
        if (maybeAudioFile.isEmpty()) {
            return;
        }
        AudioFile audioFile = maybeAudioFile.get();

        AudioWrapper wrapper = new AudioWrapper(idCount, audioFile);

        audioWrapperList.add(wrapper);

        if (wrapper.hasCover()) {
            withCover++;
        }

        idCount++;

        mainViewController.countIndicatorUp();
    }
}
