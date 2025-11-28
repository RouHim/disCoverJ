package de.itlobby.discoverj.searchengines;

import de.itlobby.discoverj.models.AudioWrapper;
import de.itlobby.discoverj.models.ImageFile;
import de.itlobby.discoverj.services.SearchQueryUtil;
import de.itlobby.discoverj.util.ImageUtil;
import de.itlobby.discoverj.util.StringUtil;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static de.itlobby.discoverj.util.WSUtil.getJsonFromUrl;

public class DeezerCoverSearchEngine implements CoverSearchEngine {

    private static final String DEEZER_API_REQUEST = "https://api.deezer.com/search?limit=5&q=";

    @Override
    public List<ImageFile> search(AudioWrapper audioWrapper) {
        String searchString = StringUtil.encodeRfc3986(SearchQueryUtil.createSearchString(audioWrapper));

        Optional<JSONObject> jsonFromUrl = getJsonFromUrl(DEEZER_API_REQUEST + searchString);
        if (jsonFromUrl.isEmpty()) {
            return Collections.emptyList();
        }

        return jsonFromUrl
                .get()
                .getJSONArray("data")
                .toList()
                .stream()
                .map(result -> new JSONObject((Map) result))
                .map(this::findCoverUrl)
                .flatMap(Optional::stream)
                .parallel()
                .map(ImageUtil::downloadImageFromUrl)
                .flatMap(Optional::stream)
                .filter(CoverSearchEngine::reachesMinRequiredCoverSize)
                .toList();
    }

    private Optional<String> findCoverUrl(JSONObject jsonObject) {
        return switch (jsonObject.getString("type")) {
            case "track" -> Optional.ofNullable(jsonObject.getJSONObject("album")).map(a -> a.getString("cover_xl"));
            case "album" -> Optional.ofNullable(jsonObject.getString("cover_xl"));
            default -> Optional.empty();
        };
    }
}
