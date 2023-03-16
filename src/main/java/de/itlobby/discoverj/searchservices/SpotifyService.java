package de.itlobby.discoverj.searchservices;

import de.itlobby.discoverj.models.AudioWrapper;
import de.itlobby.discoverj.services.SearchQueryService;
import de.itlobby.discoverj.util.ImageUtil;
import de.itlobby.discoverj.util.StringUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

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
            String searchString = URLEncoder.encode(SearchQueryService.createSearchString(audioWrapper), UTF_8);
            String searchUrl = "https://api.spotify.com/v1/search?q=%s&type=album".formatted(searchString);

            String searchResponse = getRequest(searchUrl);

            Optional<JSONArray> items = Optional.of(new JSONObject(searchResponse).getJSONObject("albums"))
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

    private String getRequest(String url) throws IOException, InterruptedException {
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + authToken)
                .build();
        HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        return httpResponse.body();
    }

    private void auth() {
        try {
            HttpClient httpClient = HttpClient.newHttpClient();
            URI uri = URI.create("https://accounts.spotify.com/api/token");
            String requestBody = "grant_type=client_credentials";
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Authorization", "Basic " + Base64.getEncoder().encodeToString((CLIENT_ID + ":" + CLIENT_SECRET).getBytes()))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            JSONObject responseJson = new JSONObject(httpResponse.body());
            this.authToken = responseJson.getString("access_token");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}