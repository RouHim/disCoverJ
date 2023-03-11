package de.itlobby.discoverj.searchservice;

import de.itlobby.discoverj.models.AudioWrapper;
import de.itlobby.discoverj.services.SearchQueryService;
import de.itlobby.discoverj.settings.AppConfig;
import de.itlobby.discoverj.settings.Settings;
import de.itlobby.discoverj.util.ImageUtil;
import de.itlobby.discoverj.util.StringUtil;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static de.itlobby.discoverj.util.WSUtil.getJsonFromUrl;
import static java.nio.charset.StandardCharsets.UTF_8;


public class SearxService implements SearchService {
    private static final int MAX_RESULTS = 10;
    private static final String API_REQUEST = "/search?categories=images&engines=bing%20images,duckduckgo%20images,google%20imagews,qwant%20images&format=json&q=";
    private static final String DEFAULT_SEARX = "https://searx.netzspielplatz.de"; // Optional: https://searx.operationtulip.com

    @Override
    public List<BufferedImage> searchCover(AudioWrapper audioWrapper) {
        AppConfig config = Settings.getInstance().getConfig();

        String googleSearchPattern = config.getGoogleSearchPattern();
        String rawQuery = SearchQueryService.createQueryFromPattern(audioWrapper, googleSearchPattern);
        String query = StringUtil.encodeRfc3986(rawQuery);
        String searxInstance = config.isSearxCustomInstanceActive() ? config.getSearxCustomInstance() : DEFAULT_SEARX;

        Optional<JSONObject> jsonFromUrl = getJsonFromUrl(searxInstance + API_REQUEST + query);
        if (jsonFromUrl.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> searchResultData = jsonFromUrl.get()
                .getJSONArray("results").toList().stream()
                .map(result -> new JSONObject((Map) result))
                .filter(result -> result.getString("category").equals("images"))
                .filter(result -> result.has("img_src"))
                .limit(MAX_RESULTS)
                .map(result -> result.getString("img_src"))
                .toList();

        Stream<BufferedImage> httpStream = searchResultData
                .parallelStream()
                .filter(url -> url.startsWith("http"))
                .map(ImageUtil::readRGBImageFromUrl)
                .flatMap(Optional::stream)
                .filter(SearchService::reachesMinRequiredCoverSize);

        Stream<BufferedImage> base64Stream = searchResultData
                .parallelStream()
                .filter(dataString -> dataString.startsWith("data") && dataString.contains("base64"))
                .map(dataString -> dataString.split("base64,")[1])
                .map(ImageUtil::readRGBImageFromBase64String)
                .flatMap(Optional::stream)
                .filter(SearchService::reachesMinRequiredCoverSize);

        return Stream.concat(httpStream, base64Stream).toList();
    }

    /**
     * Checks if the provided searx instance is valid
     *
     * @param searxInstanceUrl to check
     * @return Optional.empty() if valid, otherwise the error message that was thrown from the checked instance.
     */
    public Optional<String> checkInstance(String searxInstanceUrl) {
        try {
            JSONObject result = new JSONObject(IOUtils.toString(new URL(searxInstanceUrl + API_REQUEST + "test"), UTF_8));
            return result.has("results") ? Optional.empty() : Optional.of("Api returned no results");
        } catch (Exception e) {
            return Optional.of(e.getMessage());
        }
    }
}
