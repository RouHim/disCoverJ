package de.itlobby.discoverj.models;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor(staticName = "of")
public class LocalMatchInfo implements Serializable {
    private String path;
    private String album;
    private String albumArtist;
    private String year;
    private boolean haveCover;
}
