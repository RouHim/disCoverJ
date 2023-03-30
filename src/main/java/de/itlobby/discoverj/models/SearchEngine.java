package de.itlobby.discoverj.models;

import java.io.Serializable;

public class SearchEngine implements Serializable {
    private SearchEngineType type;
    private boolean isEnabled;

    public SearchEngine() {
    }

    public SearchEngine(SearchEngineType type, boolean isEnabled) {
        this.type = type;
        this.isEnabled = isEnabled;
    }

    public SearchEngineType getType() {
        return type;
    }

    public void setType(SearchEngineType type) {
        this.type = type;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public SearchEngine cloneSearchEngine() {
        return new SearchEngine(type, isEnabled);
    }
}