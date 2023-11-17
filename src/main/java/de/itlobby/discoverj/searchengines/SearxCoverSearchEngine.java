package de.itlobby.discoverj.searchengines;

import de.itlobby.discoverj.models.AudioWrapper;
import de.itlobby.discoverj.models.ImageFile;
import de.itlobby.discoverj.services.SearchQueryUtil;
import de.itlobby.discoverj.settings.AppConfig;
import de.itlobby.discoverj.settings.Settings;
import de.itlobby.discoverj.util.ImageUtil;
import de.itlobby.discoverj.util.SearXUtil;
import de.itlobby.discoverj.util.StringUtil;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static de.itlobby.discoverj.util.WSUtil.getJsonFromUrl;
import static java.nio.charset.StandardCharsets.UTF_8;


public class SearxCoverSearchEngine implements CoverSearchEngine {
    private static final int MAX_RESULTS = 10;
    private static final String API_REQUEST = "search?categories=images&engines=bing%20images,duckduckgo%20images,google%20images,qwant%20images&format=json&q=";
    private List<String> instances = null;

    private static List<ImageFile> startSearch(String query, String searxInstance) {
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

        Stream<ImageFile> httpStream = searchResultData
                .parallelStream()
                .filter(url -> url.startsWith("http"))
                .map(ImageUtil::downloadImageFromUrl)
                .flatMap(Optional::stream)
                .filter(CoverSearchEngine::reachesMinRequiredCoverSize);

        Stream<ImageFile> base64Stream = searchResultData
                .parallelStream()
                .filter(dataString -> dataString.startsWith("data") && dataString.contains("base64"))
                .map(dataString -> dataString.split("base64,")[1])
                .map(ImageUtil::downloadImageFromUrl)
                .flatMap(Optional::stream)
                .filter(CoverSearchEngine::reachesMinRequiredCoverSize);

        return Stream.concat(httpStream, base64Stream).toList();
    }

    @Override
    public List<ImageFile> search(AudioWrapper audioWrapper) {
        AppConfig config = Settings.getInstance().getConfig();

        String googleSearchPattern = config.getGoogleSearchPattern();
        String rawQuery = SearchQueryUtil.createSearchQueryFromPattern(audioWrapper, googleSearchPattern);
        String query = StringUtil.encodeRfc3986(rawQuery);

        // Load instances from searX index
        if (instances == null) {
            instances = SearXUtil.getInstances();
        }

        // If a manual searX instance is configured set to the start
        if (config.isSearxCustomInstanceActive()) {
            instances.add(0, config.getSearxCustomInstance());
        }

        for (String instanceUrl : instances) {
            List<ImageFile> response = startSearch(query, instanceUrl);

            if (!response.isEmpty()) {
                return response;
            }
        }

        return Collections.emptyList();
    }

    /**
     * Checks if the provided searx instance is valid
     *
     * @param searxInstanceUrl to check
     * @return Optional.empty() if valid, otherwise the error message that was thrown from the checked instance.
     */
    public Optional<String> checkInstance(String searxInstanceUrl) {
        try {
            JSONObject result = new JSONObject(IOUtils.toString(URI.create(searxInstanceUrl + API_REQUEST + "test"), UTF_8));
            return result.has("results") ? Optional.empty() : Optional.of("Api returned no results");
        } catch (Exception e) {
            return Optional.of(e.getMessage());
        }
    }
}