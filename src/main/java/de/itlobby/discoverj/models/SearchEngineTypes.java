package de.itlobby.discoverj.models;

import de.itlobby.discoverj.searchengines.CoverSearchEngine;
import de.itlobby.discoverj.searchengines.DeezerCoverSearchEngine;
import de.itlobby.discoverj.searchengines.DiscogsCoverSearchEngine;
import de.itlobby.discoverj.searchengines.ItunesCoverSearchEngine;
import de.itlobby.discoverj.searchengines.LocalCoverSearchEngine;
import de.itlobby.discoverj.searchengines.MusicbrainzCoverSearchEngine;
import de.itlobby.discoverj.searchengines.SearxCoverSearchEngine;
import de.itlobby.discoverj.searchengines.SpotifyCoverSearchEngine;
import java.io.Serializable;
import java.util.Arrays;

public enum SearchEngineTypes implements Serializable {
  ITUNES("iTunes", "logos/itunes.png", ItunesCoverSearchEngine.class, true),
  DEEZER("Deezer", "logos/deezer.png", DeezerCoverSearchEngine.class, true),
  SPOTIFY("Spotify", "logos/spotify.png", SpotifyCoverSearchEngine.class, true),
  DISCOGS("Discogs", "logos/discogs.png", DiscogsCoverSearchEngine.class, true),
  MUSICBRAINZ(
    "Musicbrainz",
    "logos/musicbrainz.png",
    MusicbrainzCoverSearchEngine.class,
    true
  ),
  SEARX("searx", "logos/searx.png", SearxCoverSearchEngine.class, true),
  LOCAL("Local", "logos/local.png", LocalCoverSearchEngine.class, true);

  private final String name;
  private final String logoPath;
  private final Class<? extends CoverSearchEngine> serviceClass;
  private final boolean returnsMultipleImages;

  SearchEngineTypes(
    String name,
    String logoPath,
    Class<? extends CoverSearchEngine> serviceClass,
    boolean returnsMultipleImages
  ) {
    this.name = name;
    this.logoPath = logoPath;
    this.serviceClass = serviceClass;
    this.returnsMultipleImages = returnsMultipleImages;
  }

  public static SearchEngineTypes getByName(String name) {
    return Arrays.stream(values())
      .filter(searchEngineTypes -> searchEngineTypes.getName().equals(name))
      .findFirst()
      .orElse(null);
  }

  public String getName() {
    return name;
  }

  public String getLogoPath() {
    return logoPath;
  }

  public Class<? extends CoverSearchEngine> getServiceClass() {
    return serviceClass;
  }

  public boolean isReturnsMultipleImages() {
    return returnsMultipleImages;
  }
}
