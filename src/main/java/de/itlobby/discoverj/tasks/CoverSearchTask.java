package de.itlobby.discoverj.tasks;

import de.itlobby.discoverj.models.AudioWrapper;
import de.itlobby.discoverj.models.SearchEngine;
import de.itlobby.discoverj.models.SearchEngineType;
import de.itlobby.discoverj.searchengines.CoverSearchEngine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

public class CoverSearchTask implements Callable<List<BufferedImage>> {
    private static final Logger log = LogManager.getLogger(CoverSearchTask.class);

    private final CoverSearchEngine coverSearchEngine;
    private final String searchEngineName;
    private final AudioWrapper audioWrapper;

    public CoverSearchTask(SearchEngine coverSearchEngine, AudioWrapper audioWrapper) {
        this.searchEngineName = coverSearchEngine.getType().getName();
        this.coverSearchEngine = createSearchEngineFromType(coverSearchEngine.getType());
        this.audioWrapper = audioWrapper;
    }

    @Override
    public List<BufferedImage> call() {
        try {
            return coverSearchEngine.search(audioWrapper);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return Collections.emptyList();
    }

    private CoverSearchEngine createSearchEngineFromType(SearchEngineType searchEngineType) {
        CoverSearchEngine coverSearchService = null;

        try {
            Class<? extends CoverSearchEngine> clazz = searchEngineType.getServiceClass();
            coverSearchService = clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return coverSearchService;

    }

    public String getAudioFileName() {
        return audioWrapper.getFileName();
    }

    public String getSearchEngineName() {
        return searchEngineName;
    }
}