package de.itlobby.discoverj.util;

import de.itlobby.discoverj.models.AudioWrapper;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class WSUtilTest {
    @Test
    void testJSONApi() {
        // GIVEN is a regular rest API url
        String url = "https://catfact.ninja/fact";

        // WHEN requesting the json response of the url
        Optional<JSONObject> jsonFromUrl = WSUtil.getJsonFromUrl(url);

        // THEN valid json should be present
        assertThat(jsonFromUrl).isNotEmpty();
    }
    @Test
    void testWebsite() {
        // GIVEN is a regular website
        String url = "https://example.org";

        // WHEN requesting the url
        Optional<JSONObject> jsonFromUrl = WSUtil.getJsonFromUrl(url);

        // THEN an empty optional should be returned
        assertThat(jsonFromUrl).isEmpty();
    }
}
