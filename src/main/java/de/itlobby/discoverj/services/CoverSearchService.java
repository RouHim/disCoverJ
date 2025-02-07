package de.itlobby.discoverj.services;

import de.itlobby.discoverj.models.AudioWrapper;
import de.itlobby.discoverj.models.ImageFile;
import de.itlobby.discoverj.models.ProgressInterruptedException;
import de.itlobby.discoverj.models.SearchEngine;
import de.itlobby.discoverj.settings.AppConfig;
import de.itlobby.discoverj.settings.Settings;
import de.itlobby.discoverj.tasks.CoverSearchTask;
import de.itlobby.discoverj.tasks.CoverSearchTaskExecutor;
import de.itlobby.discoverj.ui.core.ServiceLocator;
import de.itlobby.discoverj.util.AsyncPipeline;
import de.itlobby.discoverj.util.AudioUtil;
import de.itlobby.discoverj.util.ImageUtil;
import de.itlobby.discoverj.util.LanguageUtil;
import de.itlobby.discoverj.util.SearchEngineFuture;
import de.itlobby.discoverj.util.SystemUtil;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jaudiotagger.audio.AudioFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class CoverSearchService implements Service {
    private static final Logger log = LogManager.getLogger(CoverSearchService.class);
    private final CoverPersistentService coverPersistentService = ServiceLocator.get(CoverPersistentService.class);
    private ImageFile lastAudioCover = null;
    private volatile boolean interruptProgress;
    private volatile String lastAudioFilePath;

    public void setInterruptProgress(boolean interruptProgress) {
        this.interruptProgress = interruptProgress;
    }

    public void search() {
        // retrieve currently loaded metadata
        int audioFilesCount = DataHolder.getInstance().getAudioFilesCount();
        Map<String, List<AudioWrapper>> audioMap = DataHolder.getInstance().getAudioMap();

        // cleanup
        interruptProgress = false;
        lastAudioFilePath = null;
        lastAudioCover = null;
        ServiceLocator.unload(InitialService.class);

        // prepare ui
        getMainViewController().resetRightSide();
        getMainViewController().activateSearchState(
                event -> stopSearch(),
                audioFilesCount
        );

        // Sort audio files by path
        List<AudioWrapper> audioWrapperList = audioMap
                .values().stream()
                .flatMap(Collection::stream)
                .sorted()
                .toList();

        try {
            boolean selectCoverManually = Settings.getInstance().getConfig().isGeneralManualImageSelection();
            if (selectCoverManually) {
                AsyncPipeline
                        .run(() -> {
                            getMainViewController().showBusyIndicator(
                                    LanguageUtil.getString("CoverSearchService.loadingCovers"),
                                    () -> ServiceLocator.get(CoverSearchService.class).interruptProgress = true
                            );
                        })
                        .andThen(() -> collectAllCoverForAudioFiles(audioWrapperList))
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
     * Collects all cover images for the given list of audio files.
     * <p>
     * This method groups the audio files by their parent directory and sorts them.
     * If all audio files in a directory have the same search query, only the first
     * audio file is kept for cover search to avoid duplicate searches.
     * <p>
     * The method then flattens the grouped audio files into a list and performs
     * a parallel search for cover images for each audio file.
     *
     * @param audioWrapperList the list of audio files to collect covers for
     */
    private void collectAllCoverForAudioFiles(List<AudioWrapper> audioWrapperList) {
        // Group and sort by parent directory, and sort by parent directory
        Map<String, List<AudioWrapper>> audioMap = audioWrapperList.stream()
                .collect(Collectors.groupingBy(AudioWrapper::getParentFilePath))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        audioMap.forEach((parentPath, audioWrappers) -> {
            boolean sameSearchQuery = audioWrappers.stream()
                    .map(SearchQueryUtil::createSearchString)
                    .distinct()
                    .count() == 1;

            if (sameSearchQuery) {
                AudioWrapper first = audioWrappers.getFirst();
                audioMap.put(parentPath, List.of(first));
            }
        });


        getMainViewController().setTotalAudioCountToLoad(audioMap.size());

        // Load covers for audio files
        audioMap.values().stream()
                .flatMap(Collection::stream)
                .forEach(this::collectAllCoverForAudioFile);
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

            // Check if the first manual user image selection of this album folder was empty,
            // if so skip the manual folder selection for the rest of the folder
            boolean lastManualSelectionWasEmpty = config.isGeneralAutoLastAudio() &&
                    lastAudioFilePath != null &&
                    AudioUtil.hasSameFolderAndAlbumAsLast(audioWrapper, lastAudioFilePath) &&
                    lastAudioCover == null;
            if (lastManualSelectionWasEmpty) {
                return;
            }

            // Load from last file if possible
            if (canLoadCoverFromLastFile(config, audioWrapper)) {
                saveCoverToFile(lastAudioCover, audioWrapper);
                return;
            }

            List<ImageFile> potentialCovers = coverPersistentService.getCoversForAudioFile(audioWrapper);

            if (potentialCovers == null || potentialCovers.isEmpty()) {
                log.info("No covers to set.");
                return;
            }

            Optional<ImageFile> manuallySelectedCover = letUserManuallySelectCover(audioWrapper, potentialCovers);
            if (manuallySelectedCover.isEmpty()) {
                log.info("No valid cover selected by user");
                lastAudioCover = null;
                return;
            }

            lastAudioCover = manuallySelectedCover.get();

            saveCoverToFile(manuallySelectedCover.get(), audioWrapper);
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
            getMainViewController().setNewCoverToListItem(audioWrapper, lastAudioCover);
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
     * @param newCoverImage to set
     * @param audioWrapper  to set the new cover to
     */
    private void saveCoverToFile(ImageFile newCoverImage, AudioWrapper audioWrapper) {
        if (newCoverImage == null || audioWrapper == null) {
            return;
        }

        Optional<AudioFile> maybeAudioFile = AudioUtil.readAudioFile(audioWrapper.getFilePath());
        if (maybeAudioFile.isEmpty() || !maybeAudioFile.get().getFile().canWrite()) {
            log.error("Error while writing cover to file {}", audioWrapper.getFilePath());
            return;
        }

        AudioFile audioFile = maybeAudioFile.get();
        Optional<BufferedImage> resizedCover = resizeIfNeed(newCoverImage);

        if (resizedCover.isEmpty()) {
            log.error("Cannot resize cover image of {}", audioWrapper.getFilePath());
            return;
        }

        AppConfig config = Settings.getInstance().getConfig();
        if (config.isOverwriteOnlyHigher()
                && audioWrapper.hasCover()
                && !isNewResHigher(newCoverImage, resizedCover.get())
        ) {
            getMainViewController().setState(LanguageUtil.getString("SearchController.oldResHigher"));
        } else {
            AudioUtil.saveCoverToAudioFile(audioFile, resizedCover.get());
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

        try {
            // No need or not able to search a new cover
            boolean noNeedToSearchCover = audioWrapper.hasCover() && !config.isOverwriteCover();
            if (noNeedToSearchCover || audioWrapper.isReadOnly()) {
                SystemUtil.threadSleep(1);
                return;
            }

            getMainViewController().setEntryToProcessingState(audioWrapper);

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

    private boolean isNewResHigher(ImageFile oldCover, BufferedImage newCover) {
        double resOld = (double) oldCover.height() * (double) oldCover.width();
        double resNew = (double) newCover.getHeight() * (double) newCover.getWidth();

        return resNew > resOld;
    }

    private Optional<BufferedImage> resizeIfNeed(ImageFile imageFile) {
        try {
            BufferedImage cover = ImageIO.read(new File(imageFile.filePath()));
            int maxCoverSize = Settings.getInstance().getConfig().getMaxCoverSize();
            boolean needResize = cover.getHeight() > maxCoverSize || cover.getWidth() > maxCoverSize;
            if (!needResize) {
                return Optional.of(cover);
            }

            return Optional.ofNullable(ImageUtil.resize(cover, maxCoverSize, maxCoverSize));

        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }

        return Optional.empty();
    }

    private Optional<ImageFile> searchCoverForAudioFile(AudioWrapper audioWrapper) {
        AppConfig appConfig = Settings.getInstance().getConfig();

        List<SearchEngine> activeSearchEngines = appConfig.getSearchEngineList().stream()
                .filter(SearchEngine::isEnabled)
                .toList();

        // Iterate over all active search engines and search for covers
        // Return the first cover image that has been found
        for (SearchEngine searchEngine : activeSearchEngines) {
            CoverSearchTask coverSearchTask = new CoverSearchTask(searchEngine, audioWrapper);
            Optional<ImageFile> response = CoverSearchTaskExecutor.run(coverSearchTask, appConfig.getSearchTimeout())
                    .filter(x -> !x.isEmpty())
                    .map(x -> x.get(0));

            if (response.isPresent()) {
                return response;
            }
        }

        return Optional.empty();
    }

    private void collectAllCoverForAudioFile(AudioWrapper audioWrapper) {
        if (interruptProgress) {
            throw new ProgressInterruptedException();
        }

        List<SearchEngine> activeSearchEngines = Settings.getInstance().getConfig().getSearchEngineList()
                .stream()
                .filter(SearchEngine::isEnabled)
                .toList();

        getMainViewController().setBusyIndicatorStatusText(
                "Loading covers for: \n" + SearchQueryUtil.createSearchString(audioWrapper)
        );

        // Start cover search per active search engine
        try (ExecutorService executorService = Executors.newFixedThreadPool(activeSearchEngines.size())) {
            List<SearchEngineFuture> searchEngineFutures = activeSearchEngines.stream()
                    .map(searchEngine -> new SearchEngineFuture(
                                    executorService.submit(new CoverSearchTask(searchEngine, audioWrapper)),
                                    searchEngine.getType().getName()
                            )
                    )
                    .toList();

            int searchTimeout = Settings.getInstance().getConfig().getSearchTimeout();

            List<ImageFile> allCovers = Collections.synchronizedList(new ArrayList<>());
            for (SearchEngineFuture searchFuture : searchEngineFutures) {
                try {
                    allCovers.addAll(searchFuture.getFuture().get(searchTimeout, TimeUnit.SECONDS));
                } catch (TimeoutException e) {
                    log.error("{} seconds timeout for search engine: {}", searchTimeout, searchFuture.getName());
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }

            executorService.shutdownNow();

            coverPersistentService.persistImages(audioWrapper, allCovers);
        }

        getMainViewController().countIndicatorUp();
    }

    private Optional<ImageFile> letUserManuallySelectCover(AudioWrapper audioWrapper, List<ImageFile> images) {
        SystemUtil.requestUserAttentionInTaskbar();
        String title = SearchQueryUtil.createSearchString(audioWrapper);
        return new ImageSelectionService().openImageSelection(images, title);
    }

    public void stopSearch() {
        log.info("Stop disCoverJ search");
        interruptProgress = true;
        getMainViewController().activateActionButton(event -> search(), FontAwesomeIcon.SEARCH);
    }

}