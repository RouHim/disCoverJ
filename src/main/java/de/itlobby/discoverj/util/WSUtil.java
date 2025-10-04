package de.itlobby.discoverj.util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

public class WSUtil {

    private static final Logger log = LogManager.getLogger(WSUtil.class);

    private WSUtil() {}

    public static Optional<JSONObject> getJsonFromUrl(String url) {
        try (
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build()
        ) {
            HttpRequest request = HttpRequest.newBuilder(new URI(url)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() > 399) {
                log.warn("HTTP error: {}", response.statusCode());
                return Optional.empty();
            }

            String jsonString = response.body();

            return Optional.of(new JSONObject(jsonString));
        } catch (IllegalArgumentException e) {
            log.error(e.getMessage(), e);
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        }

        return Optional.empty();
    }
}
