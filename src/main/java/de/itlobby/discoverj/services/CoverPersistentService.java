package de.itlobby.discoverj.services;

import de.itlobby.discoverj.models.AudioWrapper;
import de.itlobby.discoverj.models.ImageFile;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CoverPersistentService implements Service {
    private final Map<AudioWrapper, List<ImageFile>> imageStore = new ConcurrentHashMap<>();

    public void persistImages(AudioWrapper audioWrapper, List<ImageFile> images) {
        imageStore.put(audioWrapper, images);
    }

    public List<ImageFile> getCoversForAudioFile(AudioWrapper audioWrapper) {
        return imageStore.get(audioWrapper);
    }

    public void cleanup() {
        imageStore.clear();
    }
}
