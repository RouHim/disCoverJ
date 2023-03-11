package de.itlobby.discoverj.ui.viewtask;

import de.itlobby.discoverj.models.AudioWrapper;
import de.itlobby.discoverj.models.ScanResultData;
import de.itlobby.discoverj.settings.Settings;
import de.itlobby.discoverj.ui.components.AudioListEntry;
import de.itlobby.discoverj.ui.viewcontroller.MainViewController;
import de.itlobby.discoverj.util.AudioUtil;
import de.itlobby.discoverj.util.SystemUtil;
import javafx.scene.image.Image;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jaudiotagger.audio.AudioFile;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class ScanFileViewTask extends ViewTask<ScanResultData> {
    private static final Logger log = LogManager.getLogger(ScanFileViewTask.class);
    private final List<String> filesToLoad;
    private final MainViewController mainViewController;
    private final int totalAudioCountToLoad;
    private final List<AudioWrapper> audioWrapperList;
    private final ScanResultData scanResultData;
    private Map<String, List<AudioWrapper>> audioData;
    private Integer idCount;
    private int withCover;
    private String parentFilePathLastScanning;

    public ScanFileViewTask(List<String> filesToLoad, MainViewController mainViewController) {
        this.filesToLoad = filesToLoad;
        this.mainViewController = mainViewController;
        totalAudioCountToLoad = filesToLoad.size();
        scanResultData = new ScanResultData();

        idCount = 0;

        audioWrapperList = new ArrayList<>();
        audioData = new HashMap<>();
    }

    @Override
    public void work() {
        if (totalAudioCountToLoad > 0) {
            filesToLoad.forEach(this::scanFile);
        }

        try {
            Collections.sort(audioWrapperList);
        } catch (Exception e) {
            log.error("ERROR while trying to sortByKey mp3 list", e);
            audioWrapperList.sort(Comparator.comparing(AudioWrapper::getFilePath));
        }


        for (AudioWrapper audioWrapper : audioWrapperList) {
            addAudioData(audioWrapper);
        }

        audioData = sortByKey(audioData);
        audioWrapperList.sort(Comparator.comparing(AudioWrapper::getFilePath));

        scanResultData.setAudioFilesCount(audioWrapperList.size());
        scanResultData.setWithCover(withCover);
        scanResultData.setAudioMap(audioData);

        if (totalAudioCountToLoad <= 300) {
            Settings.getInstance().setCoverLoadingDisabled(false);
            new Thread(() -> lazyLoadCoverImages(audioWrapperList)).start();
        } else {
            Settings.getInstance().setCoverLoadingDisabled(true);
        }

        setResult(scanResultData);
    }

    private Map<String, List<AudioWrapper>> sortByKey(Map<String, List<AudioWrapper>> audioData) {
        return audioData.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));
    }

    private void addAudioData(AudioWrapper audioWrapper) {
        String parent = audioWrapper.getParentFilePath();

        try {
            if (audioData.containsKey(parent)) {
                audioData.get(parent).add(audioWrapper);
            } else {
                audioData.put(parent, new ArrayList<>(Collections.singletonList(audioWrapper)));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void lazyLoadCoverImages(List<AudioWrapper> audioList) {
        while (mainViewController.lwAudioList.getChildren().size() < audioList.size() && !isCancelled()) {
            SystemUtil.threadSleep(100);
        }

        for (AudioWrapper audioWrapper : audioList) {
            if (isCancelled()) {
                return;
            }

            Optional<Image> coverImage = AudioUtil.getCover(audioWrapper.getFilePath(), 36, 36);

            if (coverImage.isEmpty()) {
                continue;
            }

            mainViewController.lwAudioList.getChildren()
                    .stream()
                    .filter(AudioListEntry.class::isInstance)
                    .map(AudioListEntry.class::cast)
                    .filter(x -> x.getWrapper().getId().equals(audioWrapper.getId()))
                    .findFirst()
                    .ifPresent(audioListEntry ->
                            mainViewController.createSingleLineAnimation(coverImage.get(), audioListEntry));

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
        // TODO: Here the audio file should be read the first and only time
        // May be worth doing this in parallel with a limited amount of threads
        // Read all information that are needed, and do not keep the heavy AudioFile instance in memory

        var maybeAudioFile = AudioUtil.readAudioFile(filePath);
        if (maybeAudioFile.isEmpty()) {
            return;
        }
        AudioFile audioFile = maybeAudioFile.get();

        AudioWrapper wrapper = new AudioWrapper(
                idCount,
                audioFile
        );

        // Separate the caching
        if (!wrapper.getParentFilePath().equals(parentFilePathLastScanning)) {
            // TODO: optimize cache
            parentFilePathLastScanning = wrapper.getParentFilePath();
            new Thread(() -> AudioUtil.checkForMixCD(wrapper)).start();
        }

        audioWrapperList.add(wrapper);

        if (wrapper.hasCover()) {
            withCover++;
        }

        idCount++;

        updateProgress();
    }

    private void updateProgress() {
        mainViewController.countIndicatorUp();
    }
}