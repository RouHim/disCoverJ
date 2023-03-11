package de.itlobby.discoverj.searchservice;

import de.itlobby.discoverj.models.AudioWrapper;
import de.itlobby.discoverj.services.SearchQueryService;
import de.itlobby.discoverj.util.ImageUtil;
import org.json.JSONObject;

import java.awt.image.BufferedImage;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static de.itlobby.discoverj.util.WSUtil.getJsonFromUrl;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.text.MessageFormat.format;

public class ItunesService implements SearchService {
    private static final String ITUNES_SEARCH_QUERY = "https://itunes.apple.com/search?limit=5&term={0}";

    @Override
    public List<BufferedImage> searchCover(AudioWrapper audioWrapper) {
        String searchString = URLEncoder.encode(SearchQueryService.createSearchString(audioWrapper), UTF_8);

        Optional<JSONObject> jsonFromUrl = getJsonFromUrl(
                format(ITUNES_SEARCH_QUERY, searchString)
        );

        if (jsonFromUrl.isEmpty()) {
            return Collections.emptyList();
        }

        return jsonFromUrl.get()
                .getJSONArray("results").toList().stream()
                .map(result -> new JSONObject((Map) result))
                .map(result -> result.getString("artworkUrl100"))
                .filter(imgUrl -> !imgUrl.contains("mza_"))
                .map(imgUrl -> imgUrl.replace("100x100", "1200x1200"))
                .parallel()
                .map(ImageUtil::readRGBImageFromUrl)
                .flatMap(Optional::stream)
                .filter(SearchService::reachesMinRequiredCoverSize)
                .toList();
    }
}
