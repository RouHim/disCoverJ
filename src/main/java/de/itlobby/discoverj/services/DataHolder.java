package de.itlobby.discoverj.services;

import de.itlobby.discoverj.listeners.ListenerStateProvider;
import de.itlobby.discoverj.models.AudioWrapper;
import de.itlobby.discoverj.models.ScanResultData;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DataHolder {

    private static final DataHolder instance = new DataHolder();

    private Map<String, List<AudioWrapper>> audioMap = new HashMap<>();
    private int audioFilesCount;
    private int withCoverCount;

    private DataHolder() {
    }

    public static DataHolder getInstance() {
        return instance;
    }

    public Map<String, List<AudioWrapper>> getAudioMap() {
        return audioMap;
    }

    public int getAudioFilesCount() {
        return audioFilesCount;
    }

    public int getWithCoverCount() {
        return withCoverCount;
    }

    public void updateResultEntry(AudioWrapper audioWrapper) {
        String parent = new File(audioWrapper.getFilePath()).getParent();
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
        List<AudioWrapper> audioWrappers = audioMap.get(parent);

        audioWrappers.remove(audioWrapper);

        checkForEmptyAudioDataKey();
    }

    private void checkForEmptyAudioDataKey() {
        List<String> emptyKeyList = audioMap
                .entrySet()
                .stream()
                .filter(x -> x.getValue().isEmpty())
                .map(Map.Entry::getKey)
                .toList();

        for (String key : emptyKeyList) {
            audioMap.remove(key);
            ListenerStateProvider.getInstance().getParentKeyDeletedListener().onParentListEntryDeleted(key);
        }
    }

    public List<AudioWrapper> getAudioList() {
        List<AudioWrapper> audioList = new ArrayList<>();

        audioMap.values().forEach(audioList::addAll);

        return audioList;
    }

    public void setFromScanResult(ScanResultData scanResultData) {
        this.audioMap = scanResultData.getAudioMap();
        this.audioFilesCount = scanResultData.getAudioFilesCount();
        this.withCoverCount = scanResultData.getWithCoverCount();
    }

    public void clear() {
        this.audioMap = null;
        this.withCoverCount = 0;
        this.audioFilesCount = 0;
    }

    public void removeItemsWithCover() {
        this.withCoverCount = 0;
        List<AudioWrapper> withoutCover = audioMap
                .entrySet()
                .stream()
                .flatMap(x -> x.getValue().stream())
                .filter(x -> !x.hasCover())
                .toList();

        audioMap = withoutCover
                .stream()
                .collect(Collectors.groupingBy(AudioWrapper::getParentFilePath))
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (oldValue, newValue) -> oldValue,
                                LinkedHashMap::new
                        )
                );

        this.audioFilesCount = withoutCover.size();
    }

    public void removeItemsById(List<Integer> idsToRemove) {
        List<AudioWrapper> toKeep = audioMap
                .entrySet()
                .stream()
                .flatMap(x -> x.getValue().stream())
                .filter(x -> !idsToRemove.contains(x.getId()))
                .toList();

        audioMap = toKeep
                .stream()
                .collect(Collectors.groupingBy(AudioWrapper::getParentFilePath))
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (oldValue, newValue) -> oldValue,
                                LinkedHashMap::new
                        )
                );

        this.withCoverCount = (int) toKeep.stream().filter(AudioWrapper::hasCover).count();
        this.audioFilesCount = toKeep.size();
    }
}
