package de.itlobby.discoverj.util;

import de.itlobby.discoverj.models.SearchEngine;
import de.itlobby.discoverj.models.SearchEngineType;
import de.itlobby.discoverj.settings.Settings;
import javafx.collections.ObservableList;
import javafx.scene.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ConfigUtil {
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

    public static List<SearchEngine> getActiveSearchEngines() {
        List<SearchEngine> searchEnginePriority = Settings.getInstance().getConfig().getSearchEngineList();

        return searchEnginePriority.stream().filter(SearchEngine::isEnabled).collect(Collectors.toList());
    }
}
