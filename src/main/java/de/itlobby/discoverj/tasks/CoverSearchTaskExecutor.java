package de.itlobby.discoverj.tasks;

import de.itlobby.discoverj.models.ImageFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class CoverSearchTaskExecutor {
    private static final Logger log = LogManager.getLogger(CoverSearchTaskExecutor.class);

    private CoverSearchTaskExecutor() {
    }

    public static Optional<List<ImageFile>> run(CoverSearchTask task, int timeout) {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
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
        }

        return Optional.empty();
    }
}