package de.itlobby.discoverj.models;

public class ProgressInterruptedException extends RuntimeException {
    public ProgressInterruptedException() {
    }

    public ProgressInterruptedException(Exception e) {
        super(e);
    }
}
