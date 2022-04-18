package de.itlobby.discoverj.services;

import de.itlobby.discoverj.listeners.ListenerStateProvider;
import de.itlobby.discoverj.models.ScanResultData;
import de.itlobby.discoverj.models.SimpleAudioWrapper;

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

    public void updateResultEntry(SimpleAudioWrapper simpleAudioWrapper) {
        String parent = new File(simpleAudioWrapper.getPath()).getParent();
        Map<String, List<SimpleAudioWrapper>> audioMap = scanResultData.getAudioMap();
        List<SimpleAudioWrapper> audioWrappers = audioMap.get(parent);

        int i = audioWrappers.indexOf(simpleAudioWrapper);
        audioWrappers.set(i, simpleAudioWrapper);
    }

    public void removeResultEntries(List<SimpleAudioWrapper> simpleAudioWrappers) {
        for (SimpleAudioWrapper simpleAudioWrapper : simpleAudioWrappers) {
            removeResultEntry(simpleAudioWrapper);
        }
    }

    private void removeResultEntry(SimpleAudioWrapper simpleAudioWrapper) {
        String parent = new File(simpleAudioWrapper.getPath()).getParent();
        Map<String, List<SimpleAudioWrapper>> audioMap = scanResultData.getAudioMap();
        List<SimpleAudioWrapper> audioWrappers = audioMap.get(parent);

        audioWrappers.remove(simpleAudioWrapper);

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

    public List<SimpleAudioWrapper> getAudioList() {
        List<SimpleAudioWrapper> audioList = new ArrayList<>();

        scanResultData.getAudioMap().values().forEach(audioList::addAll);

        return audioList;
    }
}
