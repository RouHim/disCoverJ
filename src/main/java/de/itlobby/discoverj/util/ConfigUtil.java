package de.itlobby.discoverj.util;

import de.itlobby.discoverj.models.SearchEngine;
import de.itlobby.discoverj.models.SearchEngineType;
import javafx.collections.ObservableList;
import javafx.scene.Node;

import java.util.ArrayList;
import java.util.List;

public class ConfigUtil {
    private ConfigUtil() {
        // hide constructor
    }

    public static List<SearchEngine> imageToSearchEngine(ObservableList<Node> children) {
        List<SearchEngine> ret = new ArrayList<>();

        for (Node child : children) {
            SearchEngineType type = SearchEngineType.getByName(child.getId().replace("img", ""));
            boolean enabled = (boolean) child.getProperties().get("ENABLED");

            SearchEngine searchEngine = new SearchEngine();
            searchEngine.setEnabled(enabled);
            searchEngine.setType(type);

            ret.add(searchEngine);
        }

        return ret;
    }

    public static List<SearchEngine> getDefaultSearchEngineList() {
        List<SearchEngine> ret = new ArrayList<>();

        for (SearchEngineType searchEngineType : SearchEngineType.values()) {
            SearchEngine searchEngine = new SearchEngine();
            searchEngine.setEnabled(true);
            searchEngine.setType(searchEngineType);
            ret.add(searchEngine);
        }

        return ret;
    }
}