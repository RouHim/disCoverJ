package de.itlobby.discoverj.models;

import de.itlobby.discoverj.util.StringUtil;

public class SearchTagWrapper {

    private String album;
    private String title;
    private String artist;
    private String fileName;

    public SearchTagWrapper(String album, String title, String artist) {
        this.album = album;
        this.title = title;
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public boolean isEmpty() {
        return StringUtil.isNullOrEmpty(album) && StringUtil.isNullOrEmpty(title) && StringUtil.isNullOrEmpty(artist);
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void clear() {
        if (hasAlbum()) {
            album = StringUtil.removeBrackets(album);
        }
        if (hasArtist()) {
            artist = StringUtil.removeBrackets(artist);
        }
        if (hasTitle()) {
            title = StringUtil.removeBrackets(title);
        }
        if (hasFileName()) {
            fileName = StringUtil.removeBrackets(fileName);
        }
    }

    public boolean hasFileName() {
        return !StringUtil.isNullOrEmpty(fileName);
    }

    public boolean hasTitle() {
        return !StringUtil.isNullOrEmpty(title);
    }

    public boolean hasArtist() {
        return !StringUtil.isNullOrEmpty(artist);
    }

    public boolean hasAlbum() {
        return !StringUtil.isNullOrEmpty(album);
    }

    public void escapeFields() {
        album = escape(album);
        title = escape(title);
        artist = escape(artist);
        fileName = escape(fileName);
    }

    private String escape(String s) {
        if (s == null) {
            return null;
        }

        String temp = s
            .replace(":", " ")
            .replace("'", " ")
            .replace(",", " ")
            .replace("*", " ")
            .replace("+", " ")
            .replace("-", " ")
            .replace("!", " ")
            .replace("?", " ")
            .replace("=", " ")
            .replace("&", " ")
            .replace("%", " ")
            .replace("$", " ")
            .replace("§", " ")
            .replace("\"", " ")
            .replace("`", " ")
            .replace("´", " ")
            .replace(".", " ");

        while (temp.contains("  ")) {
            temp = temp.replace("  ", " ");
        }

        return temp.trim();
    }
}
