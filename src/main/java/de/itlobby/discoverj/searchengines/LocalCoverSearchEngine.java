package de.itlobby.discoverj.searchengines;

import de.itlobby.discoverj.models.AudioWrapper;
import de.itlobby.discoverj.models.LocalMatchInfo;
import de.itlobby.discoverj.settings.AppConfig;
import de.itlobby.discoverj.settings.Settings;
import de.itlobby.discoverj.util.AudioUtil;
import de.itlobby.discoverj.util.ImageUtil;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;

import static de.itlobby.discoverj.util.AudioUtil.*;

public class LocalCoverSearchEngine implements CoverSearchEngine {
    private static final Map<String, List<LocalMatchInfo>> scanInfoCache = new HashMap<>();
    private final Logger log = LogManager.getLogger(this.getClass());

    @Override
    public List<BufferedImage> search(AudioWrapper audioWrapper) {
        AppConfig config = Settings.getInstance().getConfig();

        // check if we should use the cover from a custom audio folder
        // if so, we only this folder
        if (new File(config.getLocalAdditionalFolderPath()).exists()) {
            return getCoverFromCustomFolder(audioWrapper);
        }

        // First check if we have image files in the same folder
        List<BufferedImage> coverImagesFound = new ArrayList<>(getCoverFromAudioFolder(new File(audioWrapper.getParentFilePath())));

        // Additionally add images from the other audio files in the folder, if enabled in the settings
        if (config.isLocalScanAudioFiles()) {
            coverImagesFound.addAll(getCoverImagesFromOtherAudioFiles(audioWrapper));
        }

        return coverImagesFound;
    }

    private List<BufferedImage> getCoverImagesFromOtherAudioFiles(AudioWrapper audioWrapper) {
        AppConfig config = Settings.getInstance().getConfig();
        String parentFilePath = audioWrapper.getParentFilePath();

        LocalMatchInfo currentMatchInfos = new LocalMatchInfo(
                audioWrapper.getFilePath(),
                audioWrapper.getAlbum(),
                audioWrapper.getAlbumArtist(),
                audioWrapper.getYear(),
                audioWrapper.hasCover()
        );

        long s = System.currentTimeMillis();
        fillLocalScannerCache(
                new File(parentFilePath),
                audioWrapper
        );
        log.info("Built cache in: {}ms", System.currentTimeMillis() - s);

        if (!scanInfoCache.containsKey(parentFilePath)) {
            return Collections.emptyList();
        }

        s = System.currentTimeMillis();
        List<BufferedImage> foundImages = scanInfoCache.get(parentFilePath)
                .parallelStream()
                .filter(LocalMatchInfo::haveCover)
                .filter(otherFile -> matchesCriteria(currentMatchInfos, otherFile, config))
                .map(otherFile -> AudioUtil.getAudioFile(otherFile.filePath()))
                .flatMap(Optional::stream)
                .map(AudioUtil::getCoverAsBufImg)
                .flatMap(Optional::stream)
                .filter(CoverSearchEngine::reachesMinRequiredCoverSize)
                .toList();
        log.info("Matched in: {}ms", System.currentTimeMillis() - s);
        return foundImages;
    }

    private boolean matchesCriteria(LocalMatchInfo currentFile, LocalMatchInfo otherFile, AppConfig config) {
        boolean matches = !config.isLocalMatchAlbum() || currentFile.album().equals(otherFile.album());

        if (config.isLocalMatchAlbumArtist() && !currentFile.albumArtist().equals(otherFile.albumArtist())) {
            matches = false;
        }

        if (config.isLocalMatchYear() && !currentFile.year().equals(otherFile.year())) {
            matches = false;
        }

        return matches;
    }

    private List<BufferedImage> getCoverFromCustomFolder(AudioWrapper audioWrapper) {
        AppConfig config = Settings.getInstance().getConfig();
        File customDir = new File(config.getLocalAdditionalFolderPath());
        String pattern = config.getLocalNamePattern();

        String coverFileName = pattern
                .replace("%filename%", audioWrapper.getFileName())
                .replace("%artist%", audioWrapper.getArtist())
                .replace("%title%", audioWrapper.getTitle())
                .replace("%album%", audioWrapper.getAlbum())
                .replace("%dummy%", "");

        return FileUtils.listFiles(customDir, VALID_IMAGE_FILE_EXTENSION, false).stream()
                .filter(imageFile -> imageFile.getName().toLowerCase().contains(coverFileName.toLowerCase()))
                .parallel()
                .map(ImageUtil::readRGBImage)
                .flatMap(Optional::stream)
                .filter(CoverSearchEngine::reachesMinRequiredCoverSize)
                .sorted(imageSizeComparator()) // biggest image first
                .toList();
    }

    /**
     * Load images located in the same folder as the audio file.
     *
     * @param parentDir
     * @return the found list of images
     */
    private List<BufferedImage> getCoverFromAudioFolder(File parentDir) {
        return FileUtils.listFiles(parentDir, VALID_IMAGE_FILE_EXTENSION, false).stream()
                .parallel()
                .map(ImageUtil::readRGBImage)
                .flatMap(Optional::stream)
                .filter(CoverSearchEngine::reachesMinRequiredCoverSize)
                .sorted(imageSizeComparator()) // biggest image first
                .toList();
    }

    private Comparator<BufferedImage> imageSizeComparator() {
        return (x, y) -> Integer.compare(x.getHeight() * x.getWidth(), y.getHeight() * y.getWidth()) * -1;
    }

    public void fillLocalScannerCache(File parent, AudioWrapper audioWrapper) {
        if (scanInfoCache.containsKey(parent.getAbsolutePath())) {
            return;
        }

        List<LocalMatchInfo> scanInfoList = FileUtils.listFiles(parent, VALID_AUDIO_FILE_EXTENSION, false)
                .parallelStream()
                .filter(file -> !file.equals(new File(audioWrapper.getFilePath())))
                .map(this::buildCriteriaMatcher)
                .flatMap(Optional::stream)
                .toList();

        scanInfoCache.put(parent.getAbsolutePath(), scanInfoList);
    }

    private Optional<LocalMatchInfo> buildCriteriaMatcher(File file) {
        try {
            AudioFile audioFile = AudioFileIO.read(file);
            return Optional.of(new LocalMatchInfo(
                    file.getAbsolutePath(),
                    getAlbum(audioFile),
                    getAlbumArtist(audioFile),
                    getYear(audioFile),
                    haveCover(audioFile)
            ));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return Optional.empty();
    }
}