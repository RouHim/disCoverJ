package de.itlobby.discoverj.util;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;

public class WSUtil {
    private static final Logger log = LogManager.getLogger(WSUtil.class);

    private WSUtil() {
        // static class
    }

    public static Optional<JSONObject> getJsonFromUrl(String url) {
        try {
            return Optional.of(new JSONObject(IOUtils.toString(new URL(url), UTF_8)));
        } catch (IOException e) {
            log.debug(e.getMessage(), e);
        }

        return Optional.empty();
    }
}
