package de.itlobby.discoverj.models;

import java.io.Serializable;

public class SearchEngine implements Serializable {

  private SearchEngineTypes type;
  private boolean isEnabled;

  public SearchEngine() {}

  public SearchEngine(SearchEngineTypes type, boolean isEnabled) {
    this.type = type;
    this.isEnabled = isEnabled;
  }

  public SearchEngineTypes getType() {
    return type;
  }

  public void setType(SearchEngineTypes type) {
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
