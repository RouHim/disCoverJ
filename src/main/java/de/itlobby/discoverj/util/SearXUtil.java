package de.itlobby.discoverj.util;

import org.json.JSONObject;

import java.util.Collections;
import java.util.List;

import static de.itlobby.discoverj.util.WSUtil.getJsonFromUrl;

public class SearXUtil {
    private static final String SEARX_INSTANCES_INDEX = "https://searx.space/data/instances.json";

    private SearXUtil() {
    }

    public static List<String> getInstances() {
        return getJsonFromUrl(SEARX_INSTANCES_INDEX)
                .map(SearXUtil::getInstance)
                .orElse(Collections.emptyList());
    }

    private static List<String> getInstance(JSONObject response) {
        return response.getJSONObject("instances")
                .toMap()
                .keySet()
                .stream()
                .filter(instanceUrl -> !instanceUrl.endsWith(".onion/") && !instanceUrl.endsWith(".i2p/"))
                .toList();
    }
}
