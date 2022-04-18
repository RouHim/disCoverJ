package de.itlobby.discoverj.searchservice;

import de.itlobby.discoverj.models.AudioWrapper;
import de.itlobby.discoverj.settings.Settings;

import java.awt.image.BufferedImage;
import java.util.List;

public interface SearchService {
    /**
     * Checks if the minimum required cover size is reached
     *
     * @param image to check
     * @return true if the minimum size is reached, false if the cover is to small
     */
    static boolean reachesMinRequiredCoverSize(BufferedImage image) {
        int minSize = Settings.getInstance().getConfig().getMinCoverSize();
        return image.getWidth() >= minSize && image.getHeight() >= minSize;
    }

    List<BufferedImage> searchCover(AudioWrapper audioWrapper);
}
