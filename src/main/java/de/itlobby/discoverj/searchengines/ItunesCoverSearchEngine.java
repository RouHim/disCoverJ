package de.itlobby.discoverj.searchengines;

import static de.itlobby.discoverj.util.WSUtil.getJsonFromUrl;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.text.MessageFormat.format;

import de.itlobby.discoverj.models.AudioWrapper;
import de.itlobby.discoverj.models.ImageFile;
import de.itlobby.discoverj.services.SearchQueryUtil;
import de.itlobby.discoverj.util.ImageUtil;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.json.JSONObject;

public class ItunesCoverSearchEngine implements CoverSearchEngine {

  private static final String ITUNES_SEARCH_QUERY =
    "https://itunes.apple.com/search?limit=5&term={0}";

  @Override
  public List<ImageFile> search(AudioWrapper audioWrapper) {
    String searchString = URLEncoder.encode(
      SearchQueryUtil.createSearchString(audioWrapper),
      UTF_8
    );

    Optional<JSONObject> jsonFromUrl = getJsonFromUrl(
      format(ITUNES_SEARCH_QUERY, searchString)
    );

    if (jsonFromUrl.isEmpty()) {
      return Collections.emptyList();
    }

    return jsonFromUrl
      .get()
      .getJSONArray("results")
      .toList()
      .stream()
      .map(result -> new JSONObject((Map) result))
      .map(result -> result.getString("artworkUrl100"))
      .filter(imgUrl -> !imgUrl.contains("mza_"))
      .map(imgUrl -> imgUrl.replace("100x100", "1200x1200"))
      .parallel()
      .map(ImageUtil::downloadImageFromUrl)
      .flatMap(Optional::stream)
      .filter(CoverSearchEngine::reachesMinRequiredCoverSize)
      .toList();
  }
}
