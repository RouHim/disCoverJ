package de.itlobby.discoverj.mixcd;

import static de.itlobby.discoverj.util.AudioUtil.VALID_AUDIO_FILE_EXTENSION;

import de.itlobby.discoverj.util.AudioUtil;
import java.io.File;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;

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
   * @param parentFilePath the folder to check
   * @return true if the folder is a mix cd
   */
  public static boolean isMixCd(String parentFilePath) {
    return cache.computeIfAbsent(parentFilePath, __ ->
      checkForMixCD(parentFilePath)
    );
  }

  /**
   * Checks if the given folder is a mix cd.
   *
   * @param parentFilePath to analyse
   * @return true if the folder is a mix cd
   */
  static boolean checkForMixCD(String parentFilePath) {
    Collection<File> audioFilesInFolder = FileUtils.listFiles(
      new File(parentFilePath),
      VALID_AUDIO_FILE_EXTENSION,
      false
    );

    int totalSize = audioFilesInFolder.size();

    if (totalSize < 3) return false;

    // Count entries by occurrence, and get the most frequent entry
    int mostFrequentCount = audioFilesInFolder
      .parallelStream()
      .map(AudioUtil::readAudioFileSafe)
      .flatMap(audioFile -> AudioUtil.getArtists(audioFile).stream())
      .collect(
        Collectors.groupingBy(Function.identity(), Collectors.counting())
      )
      .values()
      .stream()
      .max(Comparator.naturalOrder())
      .orElse(0L)
      .intValue();

    // Calculate the percentage of the most frequent entry
    double artistToTotalPercentage =
      (double) mostFrequentCount / (double) totalSize;

    // If the percentage is less than 50%, it is a mix cd

    return artistToTotalPercentage < 0.50;
  }
}
