package de.itlobby.discoverj.searchservice;

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
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.itlobby.discoverj.util.WSUtil.getJsonFromUrl;
import static java.nio.charset.StandardCharsets.UTF_8;


public class SearxService implements SearchService {
    private static final int MAX_RESULTS = 10;
    //    private static final String DEFAULT_SEARX = "https://searx.operationtulip.com";
    private static final String API_REQUEST = "/search?categories=images&engines=bing%20images,duckduckgo%20images,google%20imagews,qwant%20images&format=json&q=";
    private static final String DEFAULT_SEARX = "https://searx.netzspielplatz.de";
    private final Logger log = LogManager.getLogger(this.getClass());

    @Override
    public List<BufferedImage> searchCover(AudioWrapper audioWrapper) {
        AppConfig config = Settings.getInstance().getConfig();

        String googleSearchPattern = config.getGoogleSearchPattern();
        String rawQuery = SearchQueryService.createQueryFromPattern(audioWrapper.getAudioFile(), googleSearchPattern);
        String query = StringUtil.encodeRfc3986(rawQuery);
        String searxInstance = config.isSearxCustomInstanceActive() ? config.getSearxCustomInstance() : DEFAULT_SEARX;

        Optional<JSONObject> jsonFromUrl = getJsonFromUrl(searxInstance + API_REQUEST + query);
        if (jsonFromUrl.isEmpty()) {
            return Collections.emptyList();
        }

        return jsonFromUrl.get()
                .getJSONArray("results").toList().stream()
                .map(result -> new JSONObject((Map) result))
                .filter(result -> result.getString("category").equals("images"))
                .filter(result -> result.has("img_src"))
                .limit(MAX_RESULTS)
                .map(result -> result.getString("img_src"))
                .parallel()
                .map(ImageUtil::getMaybeImage)
                .flatMap(Optional::stream)
                .filter(SearchService::reachesMinRequiredCoverSize)
                .collect(Collectors.toList());
    }

    /**
     * Checks if the provided hoster is valid
     *
     * @param searxHosterUrl to check
     * @return Optional.empty() if valid, otherwise the error message that was thrown from the checked instance.
     */
    public Optional<String> checkHoster(String searxHosterUrl) {
        try {
            JSONObject result = new JSONObject(IOUtils.toString(new URL(searxHosterUrl + API_REQUEST + "test"), UTF_8));
            return result.has("results") ? Optional.empty() : Optional.of("Api returned no results");
        } catch (Exception e) {
            return Optional.of(e.getMessage());
        }
    }
}
