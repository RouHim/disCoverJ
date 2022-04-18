package de.itlobby.discoverj.util;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import javax.net.ssl.SSLHandshakeException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;

public class WSUtil {
    private static final Logger log = LogManager.getLogger(WSUtil.class);

    public static JSONObject getJsonRootObject(URL url) throws IOException {
        String bodyFromURL = getStringFromURL(url, new HashMap<>());

        if (StringUtil.isNullOrEmpty(bodyFromURL)) {
            bodyFromURL = new JSONObject().toString();
        }

        return new JSONObject(bodyFromURL);
    }

    public static JSONObject getJsonRootObject(URL url, HashMap<String, String> requestProperties) throws IOException {
        String bodyFromURL = getStringFromURL(url, requestProperties);

        if (StringUtil.isNullOrEmpty(bodyFromURL)) {
            bodyFromURL = new JSONObject().toString();
        }

        return new JSONObject(bodyFromURL);
    }

    public static String getStringFromURL(String url) {
        try {
            return getStringFromURL(new URL(url), new HashMap<>());
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }

        return "";
    }

    public static String getStringFromURL(URL url, HashMap<String, String> requestProperties) throws IOException {
        StringBuilder builder = new StringBuilder();

        try {
            URLConnection connection = url.openConnection();

            for (Map.Entry<String, String> entry : requestProperties.entrySet()) {
                connection.addRequestProperty(entry.getKey(), entry.getValue());
            }

            String line;
            InputStream inputStream = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }

            inputStream.close();
        } catch (SSLHandshakeException e) {
            log.debug(e.getMessage(), e);
        }

        return builder.toString();
    }

    public static Optional<JSONObject> getJsonFromUrl(String url) {
        try {
            return Optional.of(new JSONObject(IOUtils.toString(new URL(url), UTF_8)));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }

        return Optional.empty();
    }

}
