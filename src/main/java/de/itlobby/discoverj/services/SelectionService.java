package de.itlobby.discoverj.services;

import de.itlobby.discoverj.components.AudioListEntry;
import de.itlobby.discoverj.components.FolderListEntry;
import de.itlobby.discoverj.framework.ServiceLocator;
import de.itlobby.discoverj.listeners.ListenerStateProvider;
import de.itlobby.discoverj.models.AudioWrapper;
import javafx.collections.ObservableList;
import javafx.scene.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class SelectionService implements Service {
    private final AtomicReference<AudioListEntry> lastSelected = new AtomicReference<>();
    private List<AudioListEntry> selectedEntries = Collections.synchronizedList(new ArrayList<>());
    private volatile boolean multiSelectionMode;

    public void rangeSelection(Node lastEntry) {
        if (lastSelected.get() == null) {
            addSelection(lastEntry);
        } else {
            AudioListEntry lastAudioListEntry;

            if (lastEntry instanceof AudioListEntry) {
                lastAudioListEntry = (AudioListEntry) lastEntry;

            } else {
                lastAudioListEntry = getLastFromFolder((FolderListEntry) lastEntry);
            }

            getMainViewController().highlightRangeInList(lastSelected.get(), lastAudioListEntry);
            lastSelected.set(lastAudioListEntry);
        }

        showInfo();
        checkMultiSelectionMode();
    }

    private AudioListEntry getLastFromFolder(FolderListEntry lastEntry) {
        List<AudioListEntry> entriesForFolder = getEntriesForFolder(lastEntry);
        return entriesForFolder.get(entriesForFolder.size() - 1);
    }

    public void selectAll() {
        List<AudioListEntry> allEntries = getMainViewController().lwAudioList
                .getChildren()
                .stream()
                .filter(x -> x instanceof AudioListEntry)
                .map(x -> (AudioListEntry) x)
                .collect(Collectors.toList());

        selectedEntries.clear();
        selectedEntries.addAll(allEntries);

        if (!selectedEntries.isEmpty()) {
            lastSelected.set(allEntries.get(allEntries.size() - 1));
        } else {
            lastSelected.set(null);
        }

        getMainViewController().highlightAll();
        showInfo();
        checkMultiSelectionMode();
    }

    private void checkMultiSelectionMode() {
        if (selectedEntries.size() > 1 && !multiSelectionMode) {
            ListenerStateProvider.getInstance().getMultipleSelectionListnener().onMultipleSelectionStared();
            multiSelectionMode = true;
        }
        if (selectedEntries.size() <= 1 && multiSelectionMode) {
            ListenerStateProvider.getInstance().getMultipleSelectionListnener().onMultipleSelectionEnded();
            multiSelectionMode = false;
        }
    }

    public void addSelection(Node entry) {
        if (entry instanceof AudioListEntry) {
            addSelection((AudioListEntry) entry);
        } else if (entry instanceof FolderListEntry) {
            addSelection((FolderListEntry) entry);
        }

        showInfo();
        checkMultiSelectionMode();
    }

    private void addSelection(FolderListEntry entry) {
        getEntriesForFolder(entry)
                .forEach(this::addSelection);
    }

    private void addSelection(AudioListEntry entry) {
        if (selectedEntries.contains(entry)) {
            removeSelection(entry);
            return;
        } else {
            selectedEntries.add(entry);
            getMainViewController().highlightInList(entry);
        }

        lastSelected.set(entry);
    }

    private void showInfo() {
        if (lastSelected.get() == null) {
            return;
        }

        getMainViewController().showAudioInfo(new AudioWrapper(lastSelected.get().getSimpleAudioWrapper()));
    }

    public void removeSelection(AudioListEntry entry) {
        selectedEntries.remove(entry);
        getMainViewController().unHighlightInList(entry.getSimpleAudioWrapper());

        if (lastSelected.get().equals(entry)) {
            lastSelected.set(null);

            if (!selectedEntries.isEmpty()) {
                lastSelected.set(selectedEntries.get(selectedEntries.size() - 1));
            }
        }

        checkMultiSelectionMode();
    }

    private void selectAudio(AudioListEntry audioListEntry) {
        clearAll();
        addSelection(audioListEntry);
        checkMultiSelectionMode();
    }

    public void clearAll() {
        if (multiSelectionMode) {
            ListenerStateProvider.getInstance().getMultipleSelectionListnener().onMultipleSelectionEnded();
        }

        selectedEntries.clear();
        lastSelected.set(null);
        getMainViewController().unHighlightAll();
        getMainViewController().resetRightSide();

        checkMultiSelectionMode();
    }

    public void selectFirst() {
        ObservableList<Node> children = getMainViewController().lwAudioList.getChildren();
        if (children.size() > 0) {
            Node first = children.stream().filter(x -> x instanceof AudioListEntry).findFirst().orElse(null);
            selectNode(first);
        }
    }

    public void selectLast() {
        ObservableList<Node> children = getMainViewController().lwAudioList.getChildren();
        if (children.size() > 0) {
            selectNode(children.get(children.size() - 1));
        }
    }

    public void selectRangeToHome() {
        ObservableList<Node> children = getMainViewController().lwAudioList.getChildren();
        if (children.size() > 0 && lastSelected.get() != null) {
            Node first = children.stream().filter(x -> x instanceof AudioListEntry).findFirst().orElse(null);
            rangeSelection(first);
        }
    }

    public void selectRangeToEnd() {
        ObservableList<Node> children = getMainViewController().lwAudioList.getChildren();
        if (children.size() > 0 && lastSelected.get() != null) {
            rangeSelection(children.get(children.size() - 1));
        }
    }

    public void selectUp() {
        ObservableList<Node> children = getMainViewController().lwAudioList.getChildren();
        if (children.size() > 0 && lastSelected.get() != null) {
            int i = children.indexOf(lastSelected.get());
            i--;

            if (i >= 0 && i < children.size()) {
                selectNode(children.get(i));
            }
        }
    }

    public void selectNode(Node node) {
        if (node instanceof AudioListEntry) {
            selectAudio((AudioListEntry) node);
        } else if (node instanceof FolderListEntry) {
            selectFolder((FolderListEntry) node);
        }

        showInfo();
    }

    private void selectFolder(FolderListEntry folder) {
        clearAll();
        List<AudioListEntry> entriesForFolder = getEntriesForFolder(folder);
        for (AudioListEntry audioListEntry : entriesForFolder) {
            addSelection(audioListEntry);
        }
        checkMultiSelectionMode();
    }

    private List<AudioListEntry> getEntriesForFolder(FolderListEntry folder) {
        return ServiceLocator.get(DataService.class)
                .getScanResultData()
                .getAudioMap()
                .get(folder.getPath())
                .stream()
                .map(simpleAudioWrapper -> getMainViewController().getAudioListEntry(simpleAudioWrapper))
                .collect(Collectors.toList());
    }

    public void selectDown() {
        ObservableList<Node> children = getMainViewController().lwAudioList.getChildren();
        if (children.size() > 0 && lastSelected.get() != null) {
            int i = children.indexOf(lastSelected.get());
            i++;

            if (i < children.size()) {
                selectNode(children.get(i));
            }
        }
    }

    public boolean isMultiSelectionMode() {
        return multiSelectionMode;
    }

    public AudioListEntry getLastSelected() {
        return lastSelected.get();
    }

    public List<AudioListEntry> getSelectedEntries() {
        return selectedEntries;
    }

    public void setSelectedEntries(List<AudioListEntry> selectedEntries) {
        this.selectedEntries = selectedEntries;
        checkMultiSelectionMode();
    }
}
