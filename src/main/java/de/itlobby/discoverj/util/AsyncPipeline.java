package de.itlobby.discoverj.util;

import de.itlobby.discoverj.services.ExceptionService;
import de.itlobby.discoverj.ui.core.ServiceLocator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AsyncPipeline {
    private final List<Runnable> toExecute = Collections.synchronizedList(new ArrayList<>());

    private AsyncPipeline(Runnable asyncRunnable) {
        this.toExecute.add(asyncRunnable);
    }

    public static AsyncPipeline run(Runnable runnable) {
        return new AsyncPipeline(runnable);
    }

    public AsyncPipeline andThen(Runnable runnable) {
        toExecute.add(runnable);
        return this;
    }

    /**
     * Starts the async process and runs the defined functions after completion sequential
     */
    public void begin() {
        Thread.ofVirtual()
                .uncaughtExceptionHandler(ServiceLocator.get(ExceptionService.class))
                .start(() ->
                        toExecute.forEach(runnable -> {
                            try {
                                Thread.ofVirtual()
                                        .start(runnable)
                                        .join();
                            } catch (Exception e) {
                                throw new RuntimeException(e.getMessage(), e);
                            }
                        })
                );
    }
}