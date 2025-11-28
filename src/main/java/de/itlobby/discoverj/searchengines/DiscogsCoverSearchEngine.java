package de.itlobby.discoverj.searchengines;

import static de.itlobby.discoverj.util.WSUtil.getJsonFromUrl;
import static java.nio.charset.StandardCharsets.UTF_8;

import de.itlobby.discoverj.models.AudioWrapper;
import de.itlobby.discoverj.models.ImageFile;
import de.itlobby.discoverj.services.SearchQueryUtil;
import de.itlobby.discoverj.settings.AppConfig;
import de.itlobby.discoverj.settings.Settings;
import de.itlobby.discoverj.util.AudioUtil;
import de.itlobby.discoverj.util.ImageUtil;
import de.itlobby.discoverj.util.SystemUtil;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jaudiotagger.audio.AudioFile;
import org.json.JSONObject;

public class DiscogsCoverSearchEngine implements CoverSearchEngine {

  public static final String DISCOGS_RELEASE_ID = "DISCOGS_RELEASE_ID";
  public static final String DISCOGS_API_KEY =
    "DaaGqLxNconFRhNFkhlj&secret=nHsBIligVUMbMZnUhYkNjrLecmqUIRYt";
  private final Logger log = LogManager.getLogger(this.getClass());

  @Override
  public List<ImageFile> search(AudioWrapper audioWrapper) {
    Optional<AudioFile> audioFile = AudioUtil.getAudioFile(
      audioWrapper.getFilePath()
    );

    if (audioFile.isEmpty()) {
      return Collections.emptyList();
    }

    Optional<Integer> discogsReleaseId = AudioUtil.getDiscogsReleaseId(
      audioFile.get()
    );
    if (discogsReleaseId.isPresent()) {
      return getCoverByReleaseId(discogsReleaseId.get());
    }

    return findCoverByTags(audioWrapper);
  }

  private List<ImageFile> findCoverByTags(AudioWrapper audioWrapper) {
    String searchQuery = URLEncoder.encode(
      SearchQueryUtil.createSearchString(audioWrapper),
      UTF_8
    );
    AppConfig config = Settings.getInstance().getConfig();

    String yearString = audioWrapper.getYear();
    String queryUrl = MessageFormat.format(
      "https://api.discogs.com/database/search?q={0}&key={1}&per_page=5&type=release",
      searchQuery,
      DISCOGS_API_KEY
    );

    if (config.isDiscogsUseYear() && StringUtils.isNotBlank(yearString)) {
      queryUrl += "&year=" + yearString;
    }
    if (
      config.isDiscogsUseCountry() &&
      StringUtils.isNotBlank(config.getDiscogsCountry())
    ) {
      queryUrl += "&country=" + config.getDiscogsCountry();
    }

    Optional<JSONObject> jsonFromUrl = getJsonFromUrl(queryUrl);
    if (jsonFromUrl.isEmpty()) {
      return Collections.emptyList();
    }

    return jsonFromUrl
      .get()
      .getJSONArray("results")
      .toList()
      .stream()
      .map(result -> new JSONObject((Map) result))
      .map(result -> (Integer) result.getInt("id"))
      .flatMap(releaseId -> getCoverByReleaseId(releaseId).stream())
      .toList();
  }

  private List<ImageFile> getCoverByReleaseId(Integer releaseId) {
    try {
      URI url = URI.create(
        "https://api.discogs.com/releases/%d?key=%s".formatted(
          releaseId,
          DISCOGS_API_KEY
        )
      );
      String jsonResultString = IOUtils.toString(url, UTF_8);
      return new JSONObject(jsonResultString)
        .getJSONArray("images")
        .toList()
        .stream()
        .map(result -> new JSONObject((Map) result))
        .filter(
          result ->
            result.getString("type").equals("primary") &&
            correctResolution(result)
        )
        .map(result -> result.getString("uri"))
        .map(ImageUtil::downloadImageFromUrl)
        .flatMap(Optional::stream)
        .filter(CoverSearchEngine::reachesMinRequiredCoverSize)
        .toList();
    } catch (IOException e) {
      log.error(e.getMessage(), e);
    } finally {
      // Wait a bit to avoid exceeding the api request limit
      SystemUtil.threadSleep(500);
    }

    return Collections.emptyList();
  }

  private boolean correctResolution(JSONObject image) {
    Integer minCoverSize = Settings.getInstance().getConfig().getMinCoverSize();

    Integer height = (Integer) image.get("height");
    Integer width = (Integer) image.get("width");

    return (height >= minCoverSize && width >= minCoverSize);
  }
}
