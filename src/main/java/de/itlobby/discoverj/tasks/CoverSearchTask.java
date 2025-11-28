package de.itlobby.discoverj.tasks;

import de.itlobby.discoverj.models.AudioWrapper;
import de.itlobby.discoverj.models.ImageFile;
import de.itlobby.discoverj.models.SearchEngine;
import de.itlobby.discoverj.models.SearchEngineTypes;
import de.itlobby.discoverj.searchengines.CoverSearchEngine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

public class CoverSearchTask implements Callable<List<ImageFile>> {

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
    public List<ImageFile> call() {
        try {
            return coverSearchEngine.search(audioWrapper);
        } catch (OutOfMemoryError e) {
            throw new OutOfMemoryError(e.getMessage());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return Collections.emptyList();
    }

    private CoverSearchEngine createSearchEngineFromType(SearchEngineTypes searchEngineTypes) {
        CoverSearchEngine coverSearchService = null;

        try {
            Class<? extends CoverSearchEngine> clazz = searchEngineTypes.getServiceClass();
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
