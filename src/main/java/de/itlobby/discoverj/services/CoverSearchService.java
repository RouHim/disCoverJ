package de.itlobby.discoverj.services;

import de.itlobby.discoverj.models.AudioWrapper;
import de.itlobby.discoverj.models.ProgressInterruptedException;
import de.itlobby.discoverj.models.SearchEngine;
import de.itlobby.discoverj.settings.AppConfig;
import de.itlobby.discoverj.settings.Settings;
import de.itlobby.discoverj.tasks.CoverSearchTask;
import de.itlobby.discoverj.tasks.CoverSearchTaskExecutor;
import de.itlobby.discoverj.ui.core.ServiceLocator;
import de.itlobby.discoverj.ui.helper.AsyncPipeline;
import de.itlobby.discoverj.util.AudioUtil;
import de.itlobby.discoverj.util.ImageUtil;
import de.itlobby.discoverj.util.LanguageUtil;
import de.itlobby.discoverj.util.SystemUtil;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jaudiotagger.audio.AudioFile;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.*;

public class CoverSearchService implements Service {
    private static final Logger log = LogManager.getLogger(CoverSearchService.class);
    private final CoverPersistentService coverPersistentService = ServiceLocator.get(CoverPersistentService.class);
    private BufferedImage lastAudioCover = null;
    private volatile boolean interruptProgress;
    private volatile String lastAudioFilePath;

    public void setInterruptProgress(boolean interruptProgress) {
        this.interruptProgress = interruptProgress;
    }

    public void search() {
        // retrieve currently loaded metadata
        DataHolder dataHolder = DataHolder.getInstance();

        // cleanup
        interruptProgress = false;
        lastAudioFilePath = null;
        lastAudioCover = null;
        ServiceLocator.unload(InitialService.class);

        // prepare ui
        getMainViewController().resetRightSide();
        getMainViewController().activateSearchState(
                event -> stopSearch(),
                dataHolder.getAudioFilesCount()
        );

        List<AudioWrapper> audioWrapperList = dataHolder.getAudioMap()
                .values().stream()
                .flatMap(Collection::stream)
                .sorted()
                .toList();

        try {

            boolean selectCoverManually = Settings.getInstance().getConfig().isGeneralManualImageSelection();
            if (selectCoverManually) {
                AsyncPipeline
                        .run(() -> {
                            getMainViewController().showBusyIndicator(LanguageUtil.getString("CoverSearchService.loadingCovers"));
                            getMainViewController().setTotalAudioCountToLoad(audioWrapperList.size());
                        })
                        .andThen(() -> audioWrapperList.parallelStream().forEach(this::collectAllCoverForAudioFile))
                        .andThen(() -> getMainViewController().hideBusyIndicator())
                        .andThen(() -> audioWrapperList.forEach(this::letUserSelectCover))
                        .andThen(this::finishTotal)
                        .begin();
            } else {
                AsyncPipeline
                        .run(() -> audioWrapperList.forEach(this::searchCover))
                        .andThen(this::finishTotal)
                        .begin();
            }

        } catch (ProgressInterruptedException ignored) {
            // ignore
        }
    }

    /**
     * Sets cover with manual folder selection (if cover is not assigned automatically)
     *
     * @param audioWrapper to set
     */
    private void letUserSelectCover(AudioWrapper audioWrapper) {
        AppConfig config = Settings.getInstance().getConfig();

        try {
            boolean canSetCover = audioWrapper.canWrite() && (config.isOverwriteCover() || !audioWrapper.hasCover());
            if (!canSetCover) {
                getMainViewController().setState(LanguageUtil.getString("SearchController.mp3CoverAlreadyExists"));
                return;
            }

            // Load from last file if possible
            if (canLoadCoverFromLastFile(config, audioWrapper)) {
                saveCoverToFile(lastAudioCover, audioWrapper);
                return;
            }

            List<BufferedImage> potentialCovers = coverPersistentService.getCoversForAudioFile(audioWrapper);

            if (potentialCovers.isEmpty()) {
                log.info("No covers to set.");
                return;
            }

            BufferedImage manuallySelectedCover = letUserManuallySelectCover(audioWrapper, potentialCovers);
            lastAudioCover = manuallySelectedCover;

            saveCoverToFile(manuallySelectedCover, audioWrapper);
        } finally {
            finalizeProcessAudioWrapper(audioWrapper);
        }
    }

    private void finalizeProcessAudioWrapper(AudioWrapper audioWrapper) {
        // Unset current audio information
        getMainViewController().resetCurrentAudioInformation();
        getMainViewController().setEntryToFinishedState(audioWrapper.getId());
        getMainViewController().setState("");

        // Display new-found cover on UI
        if (lastAudioCover != null) {
            WritableImage fxImage = SwingFXUtils.toFXImage(lastAudioCover, null);
            getMainViewController().setNewCoverToListItem(audioWrapper, fxImage);
        }

        lastAudioFilePath = audioWrapper.getFilePath();
    }

    private void finishTotal() {
        log.info("Search finished");

        coverPersistentService.cleanup();
        ServiceLocator.get(SelectionService.class).selectFirst();

        getMainViewController().setProgress(-1, DataHolder.getInstance().getAudioFilesCount());
        getMainViewController().activateActionButton(event -> search(), FontAwesomeIcon.SEARCH);

        SystemUtil.requestUserAttentionInTaskbar();
    }

    /**
     * Resizes and saves the found cover for the current audio file
     *
     * @param newCover     to set
     * @param audioWrapper to set the new cover to
     */
    private void saveCoverToFile(BufferedImage newCover, AudioWrapper audioWrapper) {
        if (newCover == null || audioWrapper == null) {
            return;
        }

        Optional<AudioFile> maybeAudioFile = AudioUtil.readAudioFile(audioWrapper.getFilePath());
        if (maybeAudioFile.isEmpty() || !maybeAudioFile.get().getFile().canWrite()) {
            log.error("Error while writing cover to file {}", audioWrapper.getFilePath());
            return;
        }

        AudioFile audioFile = maybeAudioFile.get();

        var resizedCover = resizeIfNeed(newCover);
        Optional<BufferedImage> oldCover = AudioUtil.getCoverAsBufImg(audioFile);

        AppConfig config = Settings.getInstance().getConfig();
        if (config.isOverwriteOnlyHigher()
                && audioWrapper.hasCover()
                && oldCover.isPresent()
                && !isNewResHigher(oldCover.get(), resizedCover)
        ) {
            getMainViewController().setState(LanguageUtil.getString("SearchController.oldResHigher"));
        } else {
            AudioUtil.saveCoverToAudioFile(audioFile, resizedCover);
        }
    }

    private boolean canLoadCoverFromLastFile(AppConfig config, AudioWrapper audioWrapper) {
        return config.isGeneralAutoLastAudio() &&
                lastAudioFilePath != null &&
                AudioUtil.hasSameFolderAndAlbumAsLast(audioWrapper, lastAudioFilePath) &&
                lastAudioCover != null;
    }

    private void searchCover(AudioWrapper audioWrapper) {
        if (interruptProgress) {
            throw new ProgressInterruptedException();
        }

        AppConfig config = Settings.getInstance().getConfig();
        getMainViewController().setEntryToProcessingState(audioWrapper);

        try {
            // No need or not able to search a new cover
            boolean noNeedToSearchCover = audioWrapper.hasCover() && !config.isOverwriteCover();
            if (noNeedToSearchCover || audioWrapper.isReadOnly()) {
                return;
            }

            // Load from last file if possible
            if (canLoadCoverFromLastFile(config, audioWrapper)) {
                saveCoverToFile(lastAudioCover, audioWrapper);
                return;
            }

            searchCoverForAudioFile(audioWrapper)
                    .ifPresentOrElse(
                            newCover -> {
                                saveCoverToFile(newCover, audioWrapper);
                                lastAudioCover = newCover;
                            },
                            () -> lastAudioCover = null
                    );

        } finally {
            finalizeProcessAudioWrapper(audioWrapper);
        }
    }

    private boolean isNewResHigher(BufferedImage oldCover, BufferedImage newCover) {
        double resOld = (double) oldCover.getHeight() * (double) oldCover.getWidth();
        double resNew = (double) newCover.getHeight() * (double) newCover.getWidth();

        return resNew > resOld;
    }

    private BufferedImage resizeIfNeed(BufferedImage cover) {
        int maxCoverSize = Settings.getInstance().getConfig().getMaxCoverSize();
        boolean needResize = cover.getHeight() > maxCoverSize || cover.getWidth() > maxCoverSize;
        if (!needResize) {
            return cover;
        }

        return ImageUtil.resize(cover, maxCoverSize, maxCoverSize);
    }

    private Optional<BufferedImage> searchCoverForAudioFile(AudioWrapper audioWrapper) {
        AppConfig appConfig = Settings.getInstance().getConfig();

        List<SearchEngine> activeSearchEngines = appConfig.getSearchEngineList().stream()
                .filter(SearchEngine::isEnabled)
                .toList();

        // Iterate over all active search engines and search for covers
        // Return the first cover image that has been found
        for (SearchEngine searchEngine : activeSearchEngines) {
            CoverSearchTask coverSearchTask = new CoverSearchTask(searchEngine, audioWrapper);
            Optional<BufferedImage> response = CoverSearchTaskExecutor.run(coverSearchTask, appConfig.getSearchTimeout())
                    .filter(x -> !x.isEmpty())
                    .map(x -> x.get(0));

            if (response.isPresent()) {
                return response;
            }
        }

        return Optional.empty();
    }

    private void collectAllCoverForAudioFile(AudioWrapper audioWrapper) {
        List<SearchEngine> activeSearchEngines = Settings.getInstance().getConfig().getSearchEngineList()
                .stream()
                .filter(SearchEngine::isEnabled)
                .toList();
        getMainViewController().setBusyIndicatorStatusText("Loading covers for: \n" + audioWrapper.getFileName());

        ExecutorService executorService = Executors.newFixedThreadPool(activeSearchEngines.size());

        List<Future<List<BufferedImage>>> searchEngineFutures = activeSearchEngines.stream()
                .map(searchEngine -> executorService.submit(new CoverSearchTask(searchEngine, audioWrapper)))
                .toList();

        int searchTimeout = Settings.getInstance().getConfig().getSearchTimeout();

        List<BufferedImage> allCovers = Collections.synchronizedList(new ArrayList<>());
        for (Future<List<BufferedImage>> searchFuture : searchEngineFutures) {
            try {
                allCovers.addAll(searchFuture.get(searchTimeout, TimeUnit.SECONDS));
            } catch (TimeoutException e) {
                log.error("{} seconds Timout for search engine {}", searchTimeout, e.getMessage());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }

        executorService.shutdownNow();

        coverPersistentService.persistImages(audioWrapper, allCovers);

        getMainViewController().countIndicatorUp();
    }

    private BufferedImage letUserManuallySelectCover(AudioWrapper audioWrapper, List<BufferedImage> images) {
        SystemUtil.requestUserAttentionInTaskbar();
        String title = SearchQueryService.createSearchString(audioWrapper);
        return new ImageSelectionService().openImageSelection(images, title);
    }

    public void stopSearch() {
        log.info("Stop disCoverJ search");
        interruptProgress = true;
        getMainViewController().activateActionButton(event -> search(), FontAwesomeIcon.SEARCH);
    }

}