package de.itlobby.discoverj.models;

import java.io.Serializable;

public record LocalMatchInfo(
        String filePath,
        String album,
        String albumArtist,
        String year,
        boolean haveCover
) implements Serializable {
}