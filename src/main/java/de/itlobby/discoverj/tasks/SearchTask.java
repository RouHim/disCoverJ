package de.itlobby.discoverj.tasks;

import de.itlobby.discoverj.models.AudioWrapper;
import de.itlobby.discoverj.searchservice.SearchService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

public class SearchTask implements Callable<List<BufferedImage>> {
    private static final Logger log = LogManager.getLogger(SearchTask.class);
    private final SearchService searchService;
    private final AudioWrapper audioWrapper;

    public SearchTask(SearchService searchService, AudioWrapper audioWrapper) {
        this.searchService = searchService;
        this.audioWrapper = audioWrapper;
    }

    @Override
    public List<BufferedImage> call() {
        try {
            return searchService.searchCover(audioWrapper);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return Collections.emptyList();
    }
}
