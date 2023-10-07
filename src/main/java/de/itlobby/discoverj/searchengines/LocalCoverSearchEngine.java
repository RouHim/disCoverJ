package de.itlobby.discoverj.searchengines;

import de.itlobby.discoverj.models.AudioWrapper;
import de.itlobby.discoverj.models.ImageFile;
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

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static de.itlobby.discoverj.util.AudioUtil.VALID_AUDIO_FILE_EXTENSION;
import static de.itlobby.discoverj.util.AudioUtil.VALID_IMAGE_FILE_EXTENSION;
import static de.itlobby.discoverj.util.AudioUtil.getAlbum;
import static de.itlobby.discoverj.util.AudioUtil.getAlbumArtist;
import static de.itlobby.discoverj.util.AudioUtil.getYear;
import static de.itlobby.discoverj.util.AudioUtil.haveCover;

public class LocalCoverSearchEngine implements CoverSearchEngine {
    private final Logger log = LogManager.getLogger(this.getClass());

    @Override
    public List<ImageFile> search(AudioWrapper audioWrapper) {
        AppConfig config = Settings.getInstance().getConfig();

        // check if we should use the cover from a custom audio folder
        // if so, we only this folder
        if (new File(config.getLocalAdditionalFolderPath()).exists()) {
            return getCoverFromCustomFolder(audioWrapper);
        }

        // First check if we have image files in the same folder
        List<ImageFile> coverImagesFound = new ArrayList<>(getCoverFromAudioFolder(new File(audioWrapper.getParentFilePath())));

        // Additionally add images from the other audio files in the folder, if enabled in the settings
        if (config.isLocalScanAudioFiles()) {
            coverImagesFound.addAll(getCoverImagesFromOtherAudioFiles(audioWrapper));
        }

        return coverImagesFound;
    }

    private List<ImageFile> getCoverImagesFromOtherAudioFiles(AudioWrapper audioWrapper) {
        AppConfig config = Settings.getInstance().getConfig();
        String parentFilePath = audioWrapper.getParentFilePath();

        LocalMatchInfo currentMatchInfos = new LocalMatchInfo(
                audioWrapper.getFilePath(),
                audioWrapper.getAlbum(),
                audioWrapper.getAlbumArtist(),
                audioWrapper.getYear(),
                audioWrapper.hasCover()
        );

        long timestamp = System.currentTimeMillis();
        List<LocalMatchInfo> filesInFolder = getFilesInFolder(new File(parentFilePath));

        List<ImageFile> foundImages = filesInFolder
                .parallelStream()
                .filter(file -> !file.filePath().equals(audioWrapper.getFilePath()))
                .filter(LocalMatchInfo::haveCover)
                .filter(otherFile -> matchesCriteria(currentMatchInfos, otherFile, config))
                .map(otherFile -> AudioUtil.getAudioFile(otherFile.filePath()))
                .flatMap(Optional::stream)
                .map(AudioUtil::getCoverAsImageFile)
                .flatMap(Optional::stream)
                .filter(CoverSearchEngine::reachesMinRequiredCoverSize)
                .toList();

        log.info("Matched in: {}ms", System.currentTimeMillis() - timestamp);

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

    private List<ImageFile> getCoverFromCustomFolder(AudioWrapper audioWrapper) {
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
                .map(ImageUtil::readImageFile)
                .flatMap(Optional::stream)
                .filter(CoverSearchEngine::reachesMinRequiredCoverSize)
                .sorted(imageSizeComparator()) // biggest image first
                .toList();
    }

    /**
     * Load images located in the same folder as the audio file.
     *
     * @param parentDir the parent directory of the audio file
     * @return the found list of images
     */
    private List<ImageFile> getCoverFromAudioFolder(File parentDir) {
        return FileUtils.listFiles(parentDir, VALID_IMAGE_FILE_EXTENSION, false)
                .parallelStream()
                .map(ImageUtil::readImageFile)
                .flatMap(Optional::stream)
                .filter(CoverSearchEngine::reachesMinRequiredCoverSize)
                .sorted(imageSizeComparator()) // biggest image first
                .toList();
    }

    private Comparator<ImageFile> imageSizeComparator() {
        return (x, y) -> Integer.compare(x.height() * x.width(), y.height() * y.width()) * -1;
    }

    public List<LocalMatchInfo> getFilesInFolder(File parent) {
        return FileUtils.listFiles(parent, VALID_AUDIO_FILE_EXTENSION, false)
                .parallelStream()
                .map(this::buildCriteriaMatcher)
                .flatMap(Optional::stream)
                .toList();
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