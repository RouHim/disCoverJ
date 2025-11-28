package de.itlobby.discoverj.searchengines;

import static de.itlobby.discoverj.util.WSUtil.getJsonFromUrl;
import static java.text.MessageFormat.format;

import de.itlobby.discoverj.mixcd.MixCd;
import de.itlobby.discoverj.models.AudioWrapper;
import de.itlobby.discoverj.models.ImageFile;
import de.itlobby.discoverj.models.SearchTagWrapper;
import de.itlobby.discoverj.settings.Settings;
import de.itlobby.discoverj.util.AudioUtil;
import de.itlobby.discoverj.util.ImageUtil;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

public class MusicbrainzCoverSearchEngine implements CoverSearchEngine {

  private static final int MAX_RESULTS = 10;

  //mbid find out / all with 95+ probability
  //http://musicbrainz.org/ws/2/release?query=release:%22meteora%22%20AND%20artist:%22linkin%20park%22&fmt=json

  //Intermediate check of the cover if necessary:
  //http://musicbrainz.org/ws/2/release/c2a975bc-90cc-414e-9415-17bffdbc191c?fmt=json

  //cover list obtained here:
  //coverartarchive.org/release/mbid
  //https://coverartarchive.org/release/9c2d7cce-7c3b-48a0-90ec-456df13f5529

  private static final Logger log = LogManager.getLogger(
    MusicbrainzCoverSearchEngine.class
  );

  public static SearchTagWrapper createSearchModel(AudioWrapper audioWrapper) {
    SearchTagWrapper query = buildString(audioWrapper);

    // Remove file extension
    if (query.isEmpty()) {
      String fileExtension = audioWrapper.getFileNameExtension();
      query.setFileName(
        audioWrapper.getFileName().replace("." + fileExtension, "")
      );
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

  @Override
  public List<ImageFile> search(AudioWrapper audioWrapper) {
    Optional<String> musicbrainzReleaseId = AudioUtil.getMusicbrainzReleaseId(
      audioWrapper
    );

    if (musicbrainzReleaseId.isPresent()) {
      return downloadCoversById(musicbrainzReleaseId.get()).toList();
    }

    String searchQuery = buildSearchQuery(audioWrapper);
    if (searchQuery == null) {
      log.info(
        "{} fills too few requirements for musicbrainz search",
        audioWrapper.getFileName()
      );
      return Collections.emptyList();
    }

    Optional<JSONObject> jsonFromUrl = getJsonFromUrl(
      format(
        "https://musicbrainz.org/ws/2/release?query={0}&fmt=json",
        searchQuery
      )
    );
    if (jsonFromUrl.isEmpty()) {
      return Collections.emptyList();
    }

    return jsonFromUrl
      .get()
      // search for all releases with a score > 95
      .getJSONArray("releases")
      .toList()
      .stream()
      .map(result -> new JSONObject((Map) result))
      .filter(entry -> entry.getInt("score") >= 95)
      .map(entry -> entry.getString("id"))
      .limit(MAX_RESULTS)
      // Check if any cover exists
      .map(id ->
        getJsonFromUrl(
          format("http://musicbrainz.org/ws/2/release/{0}?fmt=json", id)
        )
      )
      .flatMap(Optional::stream)
      .filter(
        entry -> entry.getJSONObject("cover-art-archive").getInt("count") > 0
      )
      // find their front cover urls and download them
      .flatMap(entry -> downloadCoversById(entry.getString("id")))
      .toList();
  }

  private Stream<ImageFile> downloadCoversById(String id) {
    Optional<JSONObject> jsonFromUrl = getJsonFromUrl(
      format("https://coverartarchive.org/release/{0}", id)
    );

    if (jsonFromUrl.isEmpty()) {
      return Stream.empty();
    }

    return jsonFromUrl
      .get()
      .getJSONArray("images")
      .toList()
      .parallelStream()
      .map(image -> new JSONObject((Map) image))
      .filter(image -> image.getBoolean("front"))
      .map(image -> image.getString("image"))
      // download the cover
      .map(ImageUtil::downloadImageFromUrl)
      .flatMap(Optional::stream)
      .filter(CoverSearchEngine::reachesMinRequiredCoverSize);
  }

  private String buildSearchQuery(AudioWrapper audioWrapper) {
    SearchTagWrapper searchTag = createSearchModel(audioWrapper);
    searchTag.escapeFields();

    boolean primarySingleCover = Settings.getInstance()
      .getConfig()
      .isPrimarySingleCover();
    boolean isMixCD = MixCd.isMixCd(audioWrapper.getParentFilePath());

    boolean hasArtist = searchTag.hasArtist();
    boolean hasTitle = searchTag.hasTitle();
    boolean hasAlbum = searchTag.hasAlbum();

    String query = null;
    if (primarySingleCover) {
      if (hasArtist && hasTitle) {
        query = escape(
          format(
            "release:\"{0}\" AND artist:\"{1}\"",
            searchTag.getTitle(),
            searchTag.getArtist()
          )
        );
      } else if (searchTag.hasFileName()) {
        query = escape(format("release:\"{0}\"", searchTag.getFileName()));
      }
    } else {
      if (isMixCD && hasAlbum) {
        query = escape(format("release:{0}", searchTag.getAlbum()));
      } else if (!isMixCD && hasAlbum && hasArtist) {
        query = escape(
          format(
            "release:\"{0}\" AND artist:\"{1}\"",
            searchTag.getAlbum(),
            searchTag.getArtist()
          )
        );
      } else if (hasArtist && hasTitle) {
        query = escape(
          format(
            "release:\"{0}\" AND artist:\"{1}\"",
            searchTag.getTitle(),
            searchTag.getArtist()
          )
        );
      } else if (searchTag.hasFileName()) {
        query = escape(format("release:\"{0}\"", searchTag.getFileName()));
      }
    }

    return query;
  }

  private String escape(String s) {
    return URLEncoder.encode(s, StandardCharsets.UTF_8);
  }
}
