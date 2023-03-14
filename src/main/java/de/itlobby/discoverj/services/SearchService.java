package de.itlobby.discoverj.services;

import de.itlobby.discoverj.models.AudioWrapper;
import de.itlobby.discoverj.models.ProgressInterruptedException;
import de.itlobby.discoverj.models.ScanResultData;
import de.itlobby.discoverj.models.SearchEngine;
import de.itlobby.discoverj.settings.AppConfig;
import de.itlobby.discoverj.settings.Settings;
import de.itlobby.discoverj.tasks.SearchTask;
import de.itlobby.discoverj.ui.core.ServiceLocator;
import de.itlobby.discoverj.ui.helper.AsyncAction;
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
import java.util.concurrent.atomic.AtomicReference;

public class SearchService implements Service {
    private static final Logger log = LogManager.getLogger(SearchService.class);
    private final AtomicReference<BufferedImage> lastAudioCover = new AtomicReference<>();
    private volatile boolean interruptProgress;
    private volatile String lastAudioFilePath;

    public void setInterruptProgress(boolean interruptProgress) {
        this.interruptProgress = interruptProgress;
    }

    public void search() {
        // retrieve currently loaded metadata
        ScanResultData scanResultData = ServiceLocator.get(DataService.class).getScanResultData();

        // cleanup
        interruptProgress = false;
        lastAudioFilePath = null;
        lastAudioCover.set(null);
        ServiceLocator.unload(InitialService.class);

        // prepare ui
        getMainViewController().activateSearchState(
                event -> stopSearch(),
                scanResultData.getAudioFilesCount()
        );

        try {
            AsyncAction
                    .runAsync(() -> scanResultData.getAudioMap().values().stream()
                            .flatMap(Collection::stream)
                            .forEachOrdered(this::searchCover)
                    )
                    .andThen(() -> setManualCover(scanResultData.getAudioMap()))
                    .andThen(this::finishTotal)
                    .begin();
        } catch (ProgressInterruptedException ignored) {
            // ignore
        }
    }

    /**
     * Sets cover with manual folder selection (if cover is not assigned automatically)
     *
     * @param audioMap
     */
    private void setManualCover(Map<String, List<AudioWrapper>> audioMap) {
        if (!Settings.getInstance().getConfig().isGeneralManualImageSelection()) {
            return;
        }

        audioMap.values().stream()
                .flatMap(Collection::stream)
                .forEach(audioFile -> {
                    setCover(audioFile);
                    getMainViewController().increaseProgress();
                });
    }

    private void finishTotal() {
        log.info("Search finished");

        ServiceLocator.get(CoverPersistentService.class).cleanup();
        ServiceLocator.get(SelectionService.class).selectFirst();

        getMainViewController().setProgress(0, ServiceLocator.get(DataService.class).getScanResultData().getAudioFilesCount());
        getMainViewController().activateActionButton(event -> search(), FontAwesomeIcon.SEARCH);

        SystemUtil.requestUserAttentionInTaskbar();
    }

    /**
     * Resizes and saves the found cover for the current audio file
     *
     * @param newCover     to set
     * @param audioWrapper to set the new cover to
     * @param audioWrapper to set the new cover to
     */
    private void saveCoverToFile(BufferedImage newCover, AudioWrapper audioWrapper) {
        AppConfig config = Settings.getInstance().getConfig();
        Optional<AudioFile> audioFile = AudioUtil.readAudioFile(audioWrapper.getFilePath());

        if (newCover == null || audioFile.isEmpty()) {
            getMainViewController().setState(LanguageUtil.getString("SearchController.noFittingCover"));
            getMainViewController().unHighlightInList(audioWrapper);
        } else {
            newCover = resizeIfNeed(newCover);
            Optional<BufferedImage> oldCover = AudioUtil.getCoverAsBufImg(audioFile.get());

            if (config.isOverwriteOnlyHigher()
                    && audioWrapper.hasCover()
                    && oldCover.isPresent()
                    && !isNewResHigher(oldCover.get(),
                    newCover)
            ) {
                getMainViewController().setState(LanguageUtil.getString("SearchController.oldResHigher"));
            } else {
                flushCoverImageToFile(newCover, audioWrapper, audioFile.get());
            }
        }

        lastAudioCover.set(newCover);
        lastAudioFilePath = audioWrapper.getFilePath();

        getMainViewController().unHighlightInList(audioWrapper);
        getMainViewController().setState("");
    }

    private void flushCoverImageToFile(BufferedImage newCover, AudioWrapper audioWrapper, AudioFile audioFile) {
        if (!audioFile.getFile().canWrite()) {
            AudioUtil.showCannotWriteError(audioFile.getFile().getAbsolutePath());
            return;
        }

        WritableImage fxImage = SwingFXUtils.toFXImage(newCover, null);
        getMainViewController().showNewCover(newCover, fxImage);
        getMainViewController().updateListItem(audioWrapper, fxImage);
        getMainViewController().setState(LanguageUtil.getString("SearchController.saveingMp3"));

        AudioUtil.saveCoverToAudioFile(audioFile, newCover);
    }

    /**
     * Sets the cover automatically from the last file or from manual search (if activated)
     * TODO: werde diese methode los, sie tut zwei dinge:
     * - wenn das erste cover eines albums gefunden wurde, setzt es für alle nachfolgenden tracks das gleiche cover autom.
     * - Von manual image selection wird das cover gesetzt
     *
     * @param audioWrapper to set the cover to
     */
    private void setCover(AudioWrapper audioWrapper) {
        if (interruptProgress) {
            throw new ProgressInterruptedException();
        }

        Settings settings = Settings.getInstance();
        AppConfig config = settings.getConfig();
        CoverPersistentService coverPersistentService = ServiceLocator.get(CoverPersistentService.class);

        getMainViewController().showAudioInfo(audioWrapper);
        getMainViewController().highlightInList(audioWrapper);
        getMainViewController().setAudioLineBusy(audioWrapper, true);

        if (!audioWrapper.isReadOnly() && (config.isOverwriteCover() || !audioWrapper.hasCover())) {
            boolean canLoadCoverFromLastFile = canLoadCoverFromLastFile(config, audioWrapper);

            if (canLoadCoverFromLastFile) {
                saveCoverToFile(lastAudioCover.get(), audioWrapper);
            } else {
                List<BufferedImage> covers = coverPersistentService.getCoversForAudioFile(audioWrapper);
                BufferedImage newCover = selectImageFromResultView(audioWrapper, covers);
                saveCoverToFile(newCover, audioWrapper);
            }
        } else {
            getMainViewController().setState(LanguageUtil.getString("SearchController.mp3CoverAlreadyExists"));
            Platform.runLater(() -> getMainViewController().imgNewCover.setImage(null));
        }

        getMainViewController().unHighlightInList(audioWrapper);
        getMainViewController().setAudioLineBusy(audioWrapper, false);
    }

    private boolean canLoadCoverFromLastFile(AppConfig config, AudioWrapper audioWrapper) {
        return config.isGeneralAutoLastAudio() &&
                lastAudioFilePath != null &&
                AudioUtil.hasSameFolderAndAlbumAsLast(audioWrapper, lastAudioFilePath) &&
                lastAudioCover.get() != null;
    }

    private void searchCover(AudioWrapper audioWrapper) {
        if (interruptProgress) {
            throw new ProgressInterruptedException();
        }

        AppConfig config = Settings.getInstance().getConfig();

        getMainViewController().setEntryToProcessingState(audioWrapper);

        if (!audioWrapper.hasCover() || config.isOverwriteCover()) {
            boolean canLoadCoverFromLastFile = canLoadCoverFromLastFile(config, audioWrapper);

            if (!canLoadCoverFromLastFile) {
                if (config.isGeneralManualImageSelection()) {
                    collectAllCoverForAudioFile(audioWrapper);
                } else {
                    setFirstCoverForAudioFile(audioWrapper);
                    setCover(audioWrapper);
                }
            }

            if (canLoadCoverFromLastFile && !config.isGeneralManualImageSelection()) {
                saveCoverToFile(lastAudioCover.get(), audioWrapper);
            }
        } else {
            Platform.runLater(() -> getMainViewController().imgNewCover.setImage(null));
        }

        getMainViewController().setEntryToFinishedState(audioWrapper);

        lastAudioFilePath = audioWrapper.getFilePath();
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

    /**
     * TODO: Vereine setFirstCoverForAudioFile() und collectAllCoverForAudioFile()
     *
     * @param audioWrapper
     */
    private void setFirstCoverForAudioFile(AudioWrapper audioWrapper) {
        AppConfig config = Settings.getInstance().getConfig();
        int searchTimeout = config.getSearchTimeout();
        BufferedImage newCover = null;

        List<SearchEngine> activeSearchEngines = config.getSearchEngineList()
                .stream()
                .filter(SearchEngine::isEnabled)
                .toList();

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        List<Future<List<BufferedImage>>> searchFutures = new ArrayList<>();

        for (SearchEngine searchEngine : activeSearchEngines) {
            de.itlobby.discoverj.searchservices.SearchService searchService = SystemUtil.getSearchService(searchEngine.getType());
            searchFutures.add(executorService.submit(new SearchTask(searchService, audioWrapper)));
        }

        for (Future<List<BufferedImage>> searchFuture : searchFutures) {
            try {
                List<BufferedImage> foundCovers = searchFuture.get(searchTimeout, TimeUnit.SECONDS);
                if (!foundCovers.isEmpty()) {
                    newCover = foundCovers.get(0);
                    executorService.shutdownNow();
                    break;
                }
            } catch (TimeoutException e) {
                log.error("{} seconds Timout for search engine {}", searchTimeout, e.getMessage());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }

        executorService.shutdown();

        if (newCover != null) {
            ServiceLocator.get(CoverPersistentService.class)
                    .persistImages(audioWrapper, Collections.singletonList(newCover));
        }
    }

    private void collectAllCoverForAudioFile(AudioWrapper audioWrapper) {
        final AppConfig config = Settings.getInstance().getConfig();
        List<BufferedImage> allCovers = Collections.synchronizedList(new ArrayList<BufferedImage>());
        int searchTimeout = config.getSearchTimeout();

        List<SearchEngine> activeSearchEngines = config.getSearchEngineList()
                .stream()
                .filter(SearchEngine::isEnabled)
                .toList();

        ExecutorService executorService = Executors.newFixedThreadPool(activeSearchEngines.size());
        List<Future<List<BufferedImage>>> searchFutures = new ArrayList<>();

        for (SearchEngine searchEngine : activeSearchEngines) {
            de.itlobby.discoverj.searchservices.SearchService searchService = SystemUtil.getSearchService(searchEngine.getType());
            searchFutures.add(executorService.submit(new SearchTask(searchService, audioWrapper)));
        }

        for (Future<List<BufferedImage>> searchFuture : searchFutures) {
            try {
                allCovers.addAll(searchFuture.get(searchTimeout, TimeUnit.SECONDS));
            } catch (TimeoutException e) {
                log.error("{} seconds Timout for searching", searchTimeout, e);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }

        executorService.shutdown();

        ServiceLocator.get(CoverPersistentService.class).persistImages(audioWrapper, allCovers);
    }

    private BufferedImage selectImageFromResultView(AudioWrapper audioWrapper, List<BufferedImage> images) {
        AppConfig config = Settings.getInstance().getConfig();
        boolean manualImageSelection = config.isGeneralManualImageSelection();
        BufferedImage newCover = null;

        if (manualImageSelection && !images.isEmpty()) {
            SystemUtil.requestUserAttentionInTaskbar();
            String title = SearchQueryService.createSearchString(audioWrapper);
            newCover = new ImageSelectionService().openImageSelection(images, title);
        } else if (!images.isEmpty()) {
            newCover = images.get(0);
        }

        return newCover;
    }

    public void stopSearch() {
        log.info("Stop disCoverJ search");
        interruptProgress = true;
        getMainViewController().activateActionButton(event -> search(), FontAwesomeIcon.SEARCH);
    }
}