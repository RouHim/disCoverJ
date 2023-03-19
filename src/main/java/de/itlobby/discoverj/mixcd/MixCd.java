package de.itlobby.discoverj.mixcd;

import de.itlobby.discoverj.models.AudioWrapper;
import de.itlobby.discoverj.util.CollectionUtil;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static de.itlobby.discoverj.util.AudioUtil.VALID_AUDIO_FILE_EXTENSION;

/**
 * This class is used to check if a folder is a mix cd.
 * It uses a cache to speed up the process.
 *
 * <p><b>A mix cd is:</b></p>
 * <ul>
 * <li>A folder with more than 3 audio files.</li>
 * <li>The artist of the most audio files in the folder must be less than 50% of the total audio files.</li>
 * <li>If this is the case, the folder is a mix cd.</li>
 * <li>Otherwise it is not a mix cd.</li>
 * </ul>
 */
public class MixCd {
    private static final Logger log = LogManager.getLogger(MixCd.class);

    /**
     * Cache for mix cd detection.
     */
    private static final Map<String, Boolean> cache = new ConcurrentHashMap<>();

    private MixCd() {
        // Hide constructor
    }

    /**
     * Clears the entire cache.
     */
    public static void clearCache() {
        cache.clear();
    }

    /**
     * Checks if the given folder is a mix cd.
     * Uses a cache to speed up the process.
     *
     * @param parentPath the folder to check
     * @return true if the folder is a mix cd
     */
    public static boolean isMixCd(AudioWrapper audioWrapper) {
        return cache.computeIfAbsent(audioWrapper.getParentFilePath(), __ -> checkForMixCD(audioWrapper));
    }

    /**
     * Checks if the given folder is a mix cd.
     *
     * @param audioWrapper to analyse
     * @return true if the folder is a mix cd
     */
    static boolean checkForMixCD(AudioWrapper audioWrapper) {
        Collection<File> audioFilesInFolder = FileUtils.listFiles(
                new File(audioWrapper.getParentFilePath()),
                VALID_AUDIO_FILE_EXTENSION,
                false
        );

        int totalSize = audioFilesInFolder.size();

        if (totalSize < 3) return false;

        Map<String, Long> grouped = audioWrapper
                .getArtists().stream()
                .collect(Collectors.groupingBy(x -> x, Collectors.counting()));

        Map.Entry<String, Long> firstEntry = CollectionUtil.sortByValueDescAndGetFirst(grouped);
        long biggest = firstEntry.getValue();

        double artistOfTotalPercentage = (double) biggest / (double) totalSize;

        boolean isMixCD = artistOfTotalPercentage < 0.50;

        log.debug("IsMixCD: {} ({})", isMixCD, artistOfTotalPercentage);

        return isMixCD;
    }
}
