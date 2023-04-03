package de.itlobby.discoverj.tasks;

import de.itlobby.discoverj.models.ImageFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

public class CoverSearchTaskExecutor {
    private static final Logger log = LogManager.getLogger(CoverSearchTaskExecutor.class);

    private CoverSearchTaskExecutor() {
    }

    public static Optional<List<ImageFile>> run(CoverSearchTask task, int timeout) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<List<ImageFile>> future = executor.submit(task);

        try {
            return Optional.ofNullable(future.get(timeout, TimeUnit.SECONDS));
        } catch (TimeoutException e) {
            future.cancel(true);
            log.info("Timeout searching a cover for {} from engine {}",
                    task.getAudioFileName(),
                    task.getSearchEngineName()
            );
        } catch (InterruptedException | ExecutionException __) {
            // do nothing, just go ahead returning empty response
        }

        executor.shutdownNow();

        return Optional.empty();
    }
}