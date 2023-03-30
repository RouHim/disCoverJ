package de.itlobby.discoverj.services;

import de.itlobby.discoverj.models.AudioWrapper;
import de.itlobby.discoverj.models.SearchTagWrapper;

public class SearchModelQueryService {
    private SearchModelQueryService() {
    }

    public static SearchTagWrapper createSearchModel(AudioWrapper audioWrapper) {
        SearchTagWrapper query = buildString(audioWrapper);

        // Remove file extension
        if (query.isEmpty()) {
            String fileExtension = audioWrapper.getFileNameExtension();
            query.setFileName(audioWrapper.getFileName().replace("." + fileExtension, ""));
        }

        query.clear();

        return query;
    }

    private static SearchTagWrapper buildString(AudioWrapper audioFile) {
        String album = audioFile.getAlbum();
        String title = audioFile.getTitle();
        String artist = audioFile.getArtist();

        return new SearchTagWrapper(album, title, artist);
    }
}