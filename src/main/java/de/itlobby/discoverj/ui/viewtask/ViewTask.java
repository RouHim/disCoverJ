package de.itlobby.discoverj.ui.viewtask;

public abstract class ViewTask<T> implements Runnable {

    private T result;
    private FinishedListener<T> finishedListener;
    private volatile boolean cancelled;

    protected ViewTask() {}

    @Override
    public void run() {
        work();
        finishedListener.onTaskFinished(result);
    }

    public abstract void work();

    public T getResult() {
        return result;
    }

    protected void setResult(T result) {
        this.result = result;
    }

    public void setFinishedListener(FinishedListener<T> finishedListener) {
        this.finishedListener = finishedListener;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void cancel() {
        cancelled = true;
    }

    public interface FinishedListener<T> {
        void onTaskFinished(T result);
    }
}
