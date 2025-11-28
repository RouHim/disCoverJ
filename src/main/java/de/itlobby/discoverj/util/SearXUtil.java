package de.itlobby.discoverj.util;

import static de.itlobby.discoverj.util.WSUtil.getJsonFromUrl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONObject;

public class SearXUtil {

  private static final String SEARX_INSTANCES_INDEX =
    "https://searx.space/data/instances.json";
  private static final String SEARX_MY_INSTANCE =
    "https://search.himmelstein.info/";

  private SearXUtil() {}

  public static List<String> getInstances() {
    return getJsonFromUrl(SEARX_INSTANCES_INDEX)
      .map(SearXUtil::getInstance)
      .orElse(Collections.emptyList());
  }

  private static List<String> getInstance(JSONObject response) {
    List<String> publicInstances = response
      .getJSONObject("instances")
      .toMap()
      .keySet()
      .stream()
      .filter(
        instanceUrl ->
          !instanceUrl.endsWith(".onion/") && !instanceUrl.endsWith(".i2p/")
      )
      .toList();
    List<String> instanceList = new ArrayList<>(publicInstances);
    instanceList.add(SEARX_MY_INSTANCE);
    return instanceList;
  }
}
