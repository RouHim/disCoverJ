package de.itlobby.discoverj.models;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScanResultData {
    private Map<String, List<FlatAudioWrapper>> audioMap = new HashMap<>();
    private int audioFilesCount;
    private int withCover;

    public Map<String, List<FlatAudioWrapper>> getAudioMap() {
        return audioMap;
    }

    public void setAudioMap(Map<String, List<FlatAudioWrapper>> audioMap) {
        this.audioMap = audioMap;
    }

    public int getAudioFilesCount() {
        return audioFilesCount;
    }

    public void setAudioFilesCount(int audioFilesCount) {
        this.audioFilesCount = audioFilesCount;
    }

    public int getWithCover() {
        return withCover;
    }

    public void setWithCover(int withCover) {
        this.withCover = withCover;
    }
}
