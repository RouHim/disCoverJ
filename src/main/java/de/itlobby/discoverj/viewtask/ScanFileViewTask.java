package de.itlobby.discoverj.viewtask;

import de.itlobby.discoverj.components.AudioListEntry;
import de.itlobby.discoverj.models.AudioWrapper;
import de.itlobby.discoverj.models.ScanResultData;
import de.itlobby.discoverj.models.SimpleAudioWrapper;
import de.itlobby.discoverj.settings.AppConfig;
import de.itlobby.discoverj.settings.Settings;
import de.itlobby.discoverj.util.AudioUtil;
import de.itlobby.discoverj.util.SystemUtil;
import de.itlobby.discoverj.viewcontroller.MainViewController;
import javafx.scene.image.Image;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ScanFileViewTask extends ViewTask<ScanResultData> {
    private static final Logger log = LogManager.getLogger(ScanFileViewTask.class);
    private final List<String> filesToLoad;
    private final MainViewController mainViewController;
    private final int totalAudioCountToLoad;
    private final List<AudioWrapper> audioWrapperList;
    private final ScanResultData scanResultData;
    private final AppConfig config;
    private Map<String, List<SimpleAudioWrapper>> audioData;
    private Integer idCount;
    private int withCover;
    private File parentFileLastScanning;

    public ScanFileViewTask(List<String> filesToLoad, MainViewController mainViewController) {
        this.filesToLoad = filesToLoad;
        this.mainViewController = mainViewController;
        totalAudioCountToLoad = filesToLoad.size();
        scanResultData = new ScanResultData();

        idCount = 0;

        audioWrapperList = new ArrayList<>();
        audioData = new HashMap<>();

        config = Settings.getInstance().getConfig();
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
            audioWrapperList.sort(Comparator.comparing(o -> o.getFile().getAbsolutePath()));
        }


        for (AudioWrapper audioWrapper : audioWrapperList) {
            File file = audioWrapper.getAudioFile().getFile();

            SimpleAudioWrapper simpleAudioWrapper = new SimpleAudioWrapper();
            simpleAudioWrapper.setHasCover(audioWrapper.hasCover());
            simpleAudioWrapper.setPath(file.getAbsolutePath());
            simpleAudioWrapper.setDisplayValue(AudioUtil.buildDisplayValue(audioWrapper));
            simpleAudioWrapper.setReadOnly(!audioWrapper.getAudioFile().getFile().canWrite());
            simpleAudioWrapper.setId(audioWrapper.getId());

            addAudioData(audioWrapper.getFile().getParent(), simpleAudioWrapper);
        }

        audioData = sortByKey(audioData);
        audioWrapperList.sort(Comparator.comparing(x -> x.getFile().getAbsolutePath()));

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

    private Map<String, List<SimpleAudioWrapper>> sortByKey(Map<String, List<SimpleAudioWrapper>> audioData) {
        return audioData.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));
    }

    private void addAudioData(String parent, SimpleAudioWrapper simpleAudioWrapper) {
        try {
            if (audioData.containsKey(parent)) {
                audioData.get(parent).add(simpleAudioWrapper);
            } else {
                audioData.put(parent, new ArrayList<>(Collections.singletonList(simpleAudioWrapper)));
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

            Optional<Image> coverImage = AudioUtil.getCover(audioWrapper.getAudioFile(), 36, 36);

            if (coverImage.isEmpty()) {
                continue;
            }

            mainViewController.lwAudioList.getChildren()
                    .stream()
                    .filter(x -> x instanceof AudioListEntry)
                    .map(x -> (AudioListEntry) x)
                    .filter(x -> x.getSimpleAudioWrapper().getId().equals(audioWrapper.getId()))
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

        if (!file.isDirectory()) {
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

    private void validateAndAddFile(File fileObj) {
        if (AudioUtil.isAudioFile(fileObj)) {
            try {
                addAudioToList(fileObj.getAbsolutePath());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    private void addAudioToList(String filePath) throws Exception {
        AudioWrapper wrapper = new AudioWrapper();
        File file = new File(filePath);
        File parent = file.getParentFile();
        AudioFile audioFile = AudioFileIO.read(file);
        wrapper.setAudioFile(audioFile);

        if (AudioUtil.haveCover(audioFile)) {
            wrapper.setHasCover(true);
            withCover++;
        }

        wrapper.setId(idCount);

        if (!parent.equals(parentFileLastScanning)) {
            parentFileLastScanning = parent;
            new Thread(() -> AudioUtil.checkForMixCD(wrapper)).start();
        }

        audioWrapperList.add(wrapper);
        idCount++;

        updateProgress();
    }

    private void updateProgress() {
        mainViewController.countIndicatorUp();
    }
}