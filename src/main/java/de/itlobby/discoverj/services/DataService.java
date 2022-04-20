package de.itlobby.discoverj.services;

import de.itlobby.discoverj.listeners.ListenerStateProvider;
import de.itlobby.discoverj.models.ScanResultData;
import de.itlobby.discoverj.models.FlatAudioWrapper;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DataService implements Service {
    private final Map<String, Boolean> mixCDMap = new HashMap<>();
    private ScanResultData scanResultData;

    public Map<String, Boolean> getMixCDMap() {
        return mixCDMap;
    }

    public void clearMixCDMap() {
        mixCDMap.clear();
    }

    public boolean checkForMixCDEntry(String path) {
        boolean mixCD = false;
        if (mixCDMap.containsKey(path)) {
            mixCD = mixCDMap.get(path);
        }
        return mixCD;
    }

    public ScanResultData getScanResultData() {
        return scanResultData;
    }

    public void setScanResultData(ScanResultData scanResultData) {
        this.scanResultData = scanResultData;
    }

    public void updateResultEntry(FlatAudioWrapper flatAudioWrapper) {
        String parent = new File(flatAudioWrapper.getPath()).getParent();
        Map<String, List<FlatAudioWrapper>> audioMap = scanResultData.getAudioMap();
        List<FlatAudioWrapper> audioWrappers = audioMap.get(parent);

        int i = audioWrappers.indexOf(flatAudioWrapper);
        audioWrappers.set(i, flatAudioWrapper);
    }

    public void removeResultEntries(List<FlatAudioWrapper> flatAudioWrappers) {
        for (FlatAudioWrapper flatAudioWrapper : flatAudioWrappers) {
            removeResultEntry(flatAudioWrapper);
        }
    }

    private void removeResultEntry(FlatAudioWrapper flatAudioWrapper) {
        String parent = new File(flatAudioWrapper.getPath()).getParent();
        Map<String, List<FlatAudioWrapper>> audioMap = scanResultData.getAudioMap();
        List<FlatAudioWrapper> audioWrappers = audioMap.get(parent);

        audioWrappers.remove(flatAudioWrapper);

        checkForEmptyAudioDataKey();
    }

    private void checkForEmptyAudioDataKey() {
        List<String> emptyKeyList = scanResultData.getAudioMap()
                .entrySet()
                .stream()
                .filter(x -> x.getValue().isEmpty())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());


        for (String key : emptyKeyList) {
            scanResultData.getAudioMap().remove(key);
            ListenerStateProvider.getInstance()
                    .getParentKeyDeletedListener()
                    .onParentListEntryDeleted(key);
        }
    }

    public List<FlatAudioWrapper> getAudioList() {
        List<FlatAudioWrapper> audioList = new ArrayList<>();

        scanResultData.getAudioMap().values().forEach(audioList::addAll);

        return audioList;
    }
}
