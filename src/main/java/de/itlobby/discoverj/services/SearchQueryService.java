package de.itlobby.discoverj.services;

import de.itlobby.discoverj.settings.AppConfig;
import de.itlobby.discoverj.settings.Settings;
import de.itlobby.discoverj.ui.core.ServiceLocator;
import de.itlobby.discoverj.util.AudioUtil;
import de.itlobby.discoverj.util.StringUtil;
import org.jaudiotagger.audio.AudioFile;

import java.io.File;

public class SearchQueryService {
    public static String createSearchString(AudioFile audioFile) {
        String album = AudioUtil.getAlbum(audioFile);
        String title = AudioUtil.getTitle(audioFile);
        String artist = AudioUtil.getArtist(audioFile);

        Boolean isMixCD = ServiceLocator.get(DataService.class).checkForMixCDEntry(audioFile.getFile().getParentFile().getAbsolutePath());

        String query = concatTags(album, title, artist, isMixCD);

        if (query.equals("")) {
            File file = audioFile.getFile();
            String name = file.getName();
            String fileExtension = StringUtil.getFileExtension(file);
            query = name.replace("." + fileExtension, "");
        }

        query = StringUtil.removeBrackets(query);
        query = StringUtil.removeKeyWords(query);

        return query;
    }

    private static String concatTags(String album, String title, String artist, Boolean isMixCD) {
        boolean hasAlbum = !StringUtil.isNullOrEmpty(album);
        boolean hasTitle = !StringUtil.isNullOrEmpty(title);
        boolean hasArtist = !StringUtil.isNullOrEmpty(artist);

        AppConfig config = Settings.getInstance().getConfig();

        if (!config.isGeneralAutoLastAudio() || config.isPrimarySingleCover()) {
            hasAlbum = false;
        }

        if (isMixCD && hasAlbum) {
            return album;
        }

        if (!isMixCD && hasAlbum && hasArtist) {
            return artist + " " + album;
        }

        if (hasAlbum) {
            return album;
        }

        if (hasArtist && hasTitle) {
            return artist + " " + title;
        }

        if (hasTitle) {
            return title;
        }

        return "";
    }

    public static String createQueryFromPattern(AudioFile audioFile, String pattern) {
        String query = pattern;

        query = query.replace("%auto%", createSearchString(audioFile));
        query = query.replace("%artist%", AudioUtil.getArtist(audioFile));
        query = query.replace("%title%", AudioUtil.getTitle(audioFile));
        query = query.replace("%album%", AudioUtil.getAlbum(audioFile));
        query = query.replace("%year%", AudioUtil.getYear(audioFile));

        return query;
    }
}
