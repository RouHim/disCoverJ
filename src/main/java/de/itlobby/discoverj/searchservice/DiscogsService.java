package de.itlobby.discoverj.searchservice;

import de.itlobby.discoverj.models.AudioWrapper;
import de.itlobby.discoverj.services.SearchQueryService;
import de.itlobby.discoverj.settings.AppConfig;
import de.itlobby.discoverj.settings.Settings;
import de.itlobby.discoverj.util.AudioUtil;
import de.itlobby.discoverj.util.ImageUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jaudiotagger.audio.AudioFile;
import org.json.JSONObject;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.itlobby.discoverj.util.AudioUtil.getYear;
import static de.itlobby.discoverj.util.WSUtil.getJsonFromUrl;
import static java.nio.charset.StandardCharsets.UTF_8;

public class DiscogsService implements SearchService {
    public static final String DISCOGS_RELEASE_ID = "DISCOGS_RELEASE_ID";
    public static final String DISCOGS_API_KEY = "DaaGqLxNconFRhNFkhlj&secret=nHsBIligVUMbMZnUhYkNjrLecmqUIRYt";
    private final Logger log = LogManager.getLogger(this.getClass());

    @Override
    public List<BufferedImage> searchCover(AudioWrapper audioWrapper) {
        AudioFile audioFile = audioWrapper.getAudioFile();

        Optional<Integer> discogsReleaseId = AudioUtil.getDiscogsReleaseId(audioFile);
        if (discogsReleaseId.isPresent()) {
            return getCoverByReleaseId(discogsReleaseId.get());
        }

        return findCoverByTags(audioFile);
    }

    private List<BufferedImage> findCoverByTags(AudioFile audioFile) {
        String searchQuery = URLEncoder.encode(SearchQueryService.createSearchString(audioFile), UTF_8);
        AppConfig config = Settings.getInstance().getConfig();

        String yearString = getYear(audioFile);
        String queryUrl = MessageFormat.format(
                "https://api.discogs.com/database/search?q={0}&key={1}&per_page=5&type=release",
                searchQuery,
                DISCOGS_API_KEY
        );

        if (config.isDiscogsUseYear() && StringUtils.isNotBlank(yearString)) {
            queryUrl += "&year=" + yearString;
        }
        if (config.isDiscogsUseCountry() && StringUtils.isNotBlank(config.getDiscogsCountry())) {
            queryUrl += "&country=" + config.getDiscogsCountry();
        }

        Optional<JSONObject> jsonFromUrl = getJsonFromUrl(queryUrl);
        if (jsonFromUrl.isEmpty()) {
            return Collections.emptyList();
        }

        return jsonFromUrl.get()
                .getJSONArray("results").toList().stream()
                .map(result -> new JSONObject((Map) result))
                .map(result -> result.getInt("id"))
                .flatMap(releaseId -> getCoverByReleaseId(releaseId).stream())
                .collect(Collectors.toList());
    }

    private List<BufferedImage> getCoverByReleaseId(Integer releaseId) {
        try {
            URL url = new URL("https://api.discogs.com/releases/" + releaseId + "?key=" + DISCOGS_API_KEY);
            String jsonResultString = IOUtils.toString(url, UTF_8);
            return new JSONObject(jsonResultString)
                    .getJSONArray("images").toList().stream()
                    .map(result -> new JSONObject((Map) result))
                    .filter(result -> result.getString("type").equals("primary") && correctResolution(result))
                    .map(result -> result.getString("uri"))
                    .map(ImageUtil::readRGBImageFromUrl)
                    .flatMap(Optional::stream)
                    .filter(SearchService::reachesMinRequiredCoverSize)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error(e.getMessage(), e);
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
