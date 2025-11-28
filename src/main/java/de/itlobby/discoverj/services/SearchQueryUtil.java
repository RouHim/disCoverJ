package de.itlobby.discoverj.services;

import de.itlobby.discoverj.mixcd.MixCd;
import de.itlobby.discoverj.models.AudioWrapper;
import de.itlobby.discoverj.settings.AppConfig;
import de.itlobby.discoverj.settings.Settings;
import de.itlobby.discoverj.util.StringUtil;

public class SearchQueryUtil {

    private SearchQueryUtil() {
    }

    public static String createSearchString(AudioWrapper audioWrapper) {
        String album = audioWrapper.getAlbum();
        String title = audioWrapper.getTitle();
        String artist = audioWrapper.getArtist();

        boolean isMixCD = MixCd.isMixCd(audioWrapper.getParentFilePath());

        String query = concatTags(album, title, artist, isMixCD);

        if (query.equals("")) {
            String name = audioWrapper.getFileName();
            String fileExtension = audioWrapper.getFileNameExtension();
            query = name.replace("." + fileExtension, "");
        }

        query = StringUtil.removeBrackets(query);
        query = StringUtil.removeKeyWords(query);

        return query;
    }

    private static String concatTags(String album, String title, String artist, boolean isMixCD) {
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

    public static String createSearchQueryFromPattern(AudioWrapper audioWrapper, String pattern) {
        String query = pattern;

        query = query.replace("%auto%", createSearchString(audioWrapper));
        query = query.replace("%artist%", audioWrapper.getArtist());
        query = query.replace("%title%", audioWrapper.getTitle());
        query = query.replace("%album%", audioWrapper.getAlbum());
        query = query.replace("%year%", audioWrapper.getYear());

        return query;
    }
}
