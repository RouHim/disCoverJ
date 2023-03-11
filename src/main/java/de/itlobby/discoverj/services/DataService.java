package de.itlobby.discoverj.services;

import de.itlobby.discoverj.listeners.ListenerStateProvider;
import de.itlobby.discoverj.models.AudioWrapper;
import de.itlobby.discoverj.models.ScanResultData;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DataService implements Service {
    private static final Map<String, Boolean> mixCDCache = new ConcurrentHashMap<>();
    private ScanResultData scanResultData;

    public Map<String, Boolean> getMixCDCache() {
        return mixCDCache;
    }

    public void clearMixCDCache() {
        mixCDCache.clear();
    }

    public boolean checkForMixCDEntry(String path) {
        boolean mixCD = false;
        if (mixCDCache.containsKey(path)) {
            mixCD = mixCDCache.get(path);
        }
        return mixCD;
    }

    public ScanResultData getScanResultData() {
        return scanResultData;
    }

    public void setScanResultData(ScanResultData scanResultData) {
        this.scanResultData = scanResultData;
    }

    public void updateResultEntry(AudioWrapper audioWrapper) {
        String parent = new File(audioWrapper.getFilePath()).getParent();
        Map<String, List<AudioWrapper>> audioMap = scanResultData.getAudioMap();
        List<AudioWrapper> audioWrappers = audioMap.get(parent);

        int i = audioWrappers.indexOf(audioWrapper);
        audioWrappers.set(i, audioWrapper);
    }

    public void removeResultEntries(List<AudioWrapper> audioWrappers) {
        for (AudioWrapper audioWrapper : audioWrappers) {
            removeResultEntry(audioWrapper);
        }
    }

    private void removeResultEntry(AudioWrapper audioWrapper) {
        String parent = new File(audioWrapper.getFilePath()).getParent();
        Map<String, List<AudioWrapper>> audioMap = scanResultData.getAudioMap();
        List<AudioWrapper> audioWrappers = audioMap.get(parent);

        audioWrappers.remove(audioWrapper);

        checkForEmptyAudioDataKey();
    }

    private void checkForEmptyAudioDataKey() {
        List<String> emptyKeyList = scanResultData.getAudioMap()
                .entrySet()
                .stream()
                .filter(x -> x.getValue().isEmpty())
                .map(Map.Entry::getKey)
                .toList();


        for (String key : emptyKeyList) {
            scanResultData.getAudioMap().remove(key);
            ListenerStateProvider.getInstance()
                    .getParentKeyDeletedListener()
                    .onParentListEntryDeleted(key);
        }
    }

    public List<AudioWrapper> getAudioList() {
        List<AudioWrapper> audioList = new ArrayList<>();

        scanResultData.getAudioMap().values().forEach(audioList::addAll);

        return audioList;
    }
}