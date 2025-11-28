package de.itlobby.discoverj.models;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScanResultData {

  private Map<String, List<AudioWrapper>> audioMap = new HashMap<>();
  private int audioFilesCount;
  private int withCoverCount;

  public ScanResultData(
    Map<String, List<AudioWrapper>> audioMap,
    int audioFilesCount,
    int withCoverCount
  ) {
    this.audioMap = audioMap;
    this.audioFilesCount = audioFilesCount;
    this.withCoverCount = withCoverCount;
  }

  public Map<String, List<AudioWrapper>> getAudioMap() {
    return audioMap;
  }

  public void setAudioMap(Map<String, List<AudioWrapper>> audioMap) {
    this.audioMap = audioMap;
  }

  public int getAudioFilesCount() {
    return audioFilesCount;
  }

  public void setAudioFilesCount(int audioFilesCount) {
    this.audioFilesCount = audioFilesCount;
  }

  public int getWithCoverCount() {
    return withCoverCount;
  }

  public void setWithCoverCount(int withCoverCount) {
    this.withCoverCount = withCoverCount;
  }
}
