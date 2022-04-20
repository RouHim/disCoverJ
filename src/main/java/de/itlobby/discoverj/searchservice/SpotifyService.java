package de.itlobby.discoverj.searchservice;

import de.itlobby.discoverj.models.AudioWrapper;
import de.itlobby.discoverj.services.SearchQueryService;
import de.itlobby.discoverj.util.ImageUtil;
import de.itlobby.discoverj.util.StringUtil;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.image.BufferedImage;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;

public class SpotifyService implements SearchService {
    private static final String CLIENT_ID = "ba25a9ff00bc4e19bf598dbe55c041ea";
    private static final String CLIENT_SECRET = "0465053e651b4dfb87a0b60571c5aea1";
    private static final Logger log = LogManager.getLogger(SpotifyService.class);
    private String authToken;

    public SpotifyService() {
        auth();
    }

    private static Optional<String> getCoverUrl(JSONObject ret) {
        return Optional.ofNullable(ret.getJSONArray("images"))
                .map(images -> images.getJSONObject(0))
                .map(firstImage -> firstImage.getString("url"));
    }

    @Override
    public List<BufferedImage> searchCover(AudioWrapper audioWrapper) {
        if (StringUtil.isNullOrEmpty(authToken)) {
            return Collections.emptyList();
        }

        try {
            String searchString = URLEncoder.encode(SearchQueryService.createSearchString(audioWrapper.getAudioFile()), UTF_8);
            String urlString = String.format("https://api.spotify.com/v1/search?q=%s&type=album", searchString);

            String jsonString = Unirest.get(urlString)
                    .header("Authorization", "Bearer " + authToken)
                    .asString().getBody();

            Optional<JSONArray> items = Optional.of(new JSONObject(jsonString).getJSONObject("albums"))
                    .map(albums -> albums.getJSONArray("items"));

            if (items.isEmpty()) {
                return Collections.emptyList();
            }

            return items.get().toList().stream()
                    .map(result -> new JSONObject((Map) result))
                    .map(SpotifyService::getCoverUrl)
                    .flatMap(Optional::stream)
                    .map(ImageUtil::readRGBImageFromUrl)
                    .flatMap(Optional::stream)
                    .filter(SearchService::reachesMinRequiredCoverSize)
                    .toList();
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
        }

        return Collections.emptyList();
    }

    private void auth() {
        try {
            HttpResponse<JsonNode> jsonNodeHttpResponse = Unirest.post("https://accounts.spotify.com/api/token")
                    .basicAuth(CLIENT_ID, CLIENT_SECRET)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .field("grant_type", "client_credentials")
                    .asJson();

            this.authToken = jsonNodeHttpResponse
                    .getBody()
                    .getObject()
                    .getString("access_token");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
