package de.itlobby.discoverj.services;

import de.itlobby.discoverj.models.SearchTagWrapper;
import de.itlobby.discoverj.util.AudioUtil;
import de.itlobby.discoverj.util.StringUtil;
import org.jaudiotagger.audio.AudioFile;

import java.io.File;

public class SearchModelQueryService {
    public static SearchTagWrapper createSearchModel(AudioFile audioFile) {
        SearchTagWrapper query = buildString(audioFile);

        if (query.isEmpty()) {
            File file = audioFile.getFile();
            String name = file.getName();
            String fileExtension = StringUtil.getFileExtension(file);
            query.setFileName(name.replace("." + fileExtension, ""));
        }

        query.clear();

        return query;
    }

    private static SearchTagWrapper buildString(AudioFile audioFile) {
        String album = AudioUtil.getAlbum(audioFile);
        String title = AudioUtil.getTitle(audioFile);
        String artist = AudioUtil.getArtist(audioFile);

        return new SearchTagWrapper(album, title, artist);
    }
}
