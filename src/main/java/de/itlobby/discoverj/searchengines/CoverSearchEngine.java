package de.itlobby.discoverj.searchengines;

import de.itlobby.discoverj.models.AudioWrapper;
import de.itlobby.discoverj.models.ImageFile;
import de.itlobby.discoverj.settings.Settings;

import java.util.List;

public interface CoverSearchEngine {
    /**
     * Checks if the minimum required cover size is reached
     *
     * @param image to check
     * @return true if the minimum size is reached, false if the cover is too small
     */
    static boolean reachesMinRequiredCoverSize(ImageFile image) {
        int minSize = Settings.getInstance().getConfig().getMinCoverSize();
        return image.width() >= minSize && image.height()
                >= minSize;
    }

    /**
     * Searches for the given audio wrapper covers
     *
     * @param audioWrapper to find cover for
     * @return the found covers, or empty list if nothing was found
     */
    List<ImageFile> search(AudioWrapper audioWrapper);
}