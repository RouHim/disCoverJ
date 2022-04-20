package de.itlobby.discoverj.ui.helper;

import de.itlobby.discoverj.services.ExceptionService;
import de.itlobby.discoverj.ui.core.ServiceLocator;

import java.util.ArrayList;
import java.util.List;

public class AsyncAction {
    private final Runnable asyncRunnable;
    private final List<Runnable> onThreadFinishedRunnables = new ArrayList<>();

    private AsyncAction(Runnable asyncRunnable) {
        this.asyncRunnable = asyncRunnable;
    }

    public static AsyncAction runAsync(Runnable asyncRunnable) {
        return new AsyncAction(asyncRunnable);
    }

    public AsyncAction andThen(Runnable c) {
        onThreadFinishedRunnables.add(c);
        return this;
    }

    /**
     * Starts the async process and runs the defined functions after completion sequential
     */
    public void begin() {
        Thread thread = new Thread(() -> {
            asyncRunnable.run();
            onThreadFinishedRunnables.forEach(Runnable::run);
        });
        thread.setUncaughtExceptionHandler(ServiceLocator.get(ExceptionService.class));
        thread.start();
    }
}
