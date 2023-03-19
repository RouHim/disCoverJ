package de.itlobby.discoverj.searchservices;

import de.itlobby.discoverj.models.AudioWrapper;
import de.itlobby.discoverj.services.SearchQueryService;
import de.itlobby.discoverj.settings.AppConfig;
import de.itlobby.discoverj.settings.Settings;
import de.itlobby.discoverj.util.ImageUtil;
import de.itlobby.discoverj.util.StringUtil;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.awt.image.BufferedImage;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.itlobby.discoverj.util.WSUtil.getJsonFromUrl;
import static java.nio.charset.StandardCharsets.UTF_8;


public class SearxService implements SearchService {
    private static final Logger log = LogManager.getLogger(SearxService.class);

    private static final int MAX_RESULTS = 10;
    private static final String SEARX_INSTANCES_INDEX = "https://searx.space/data/instances.json";
    private static final String API_REQUEST = "search?categories=images&engines=bing%20images,duckduckgo%20images,google%20images,qwant%20images&format=json&q=";
    private List<String> instances = null;

    @Override
    public List<BufferedImage> searchCover(AudioWrapper audioWrapper) {
        AppConfig config = Settings.getInstance().getConfig();

        String googleSearchPattern = config.getGoogleSearchPattern();
        String rawQuery = SearchQueryService.createQueryFromPattern(audioWrapper, googleSearchPattern);
        String query = StringUtil.encodeRfc3986(rawQuery);

        // Load instances from searX index
        if (instances == null) {
            instances = getJsonFromUrl(SEARX_INSTANCES_INDEX)
                    .map(response -> response.getJSONObject("instances").toMap().keySet().stream().toList())
                    .orElse(Collections.emptyList());
        }

        // If a manual searX instance is configured set to the start
        if (config.isSearxCustomInstanceActive()) {
            instances.add(0, config.getSearxCustomInstance());
        }

        for (String instanceUrl : instances) {
            List<BufferedImage> response = startSearch(query, instanceUrl);

            if (!response.isEmpty()) {
                return response;
            }
        }

        return Collections.emptyList();
    }

    private List<String> detectEngines() {
        Optional<JSONObject> jsonFromUrl = getJsonFromUrl(SEARX_INSTANCES_INDEX);
        if (jsonFromUrl.isEmpty()) {
            return Collections.emptyList();
        }
        return jsonFromUrl.get().getJSONObject("instances").toMap().keySet()
                .parallelStream()
                .filter(SearxService::isReachable)
                .collect(Collectors.toList());
    }

    private static List<BufferedImage> startSearch(String query, String searxInstance) {
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

    private static boolean isReachable(String engineUrl) {
        HttpURLConnection connection = null;
        try {
            URL headUrl = new URL(engineUrl + API_REQUEST + "test");
            connection = (HttpURLConnection) headUrl.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            return connection.getResponseCode() == HttpURLConnection.HTTP_OK;
        } catch (Exception e) {
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
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