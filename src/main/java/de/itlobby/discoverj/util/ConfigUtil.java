package de.itlobby.discoverj.util;

import de.itlobby.discoverj.models.SearchEngine;
import de.itlobby.discoverj.models.SearchEngineTypes;
import javafx.collections.ObservableList;
import javafx.scene.Node;

import java.util.Arrays;
import java.util.List;

public class ConfigUtil {

    private ConfigUtil() {
        // hide constructor
    }

    public static List<SearchEngine> imageToSearchEngine(ObservableList<Node> children) {
        return children
                .stream()
                .map(child ->
                        new SearchEngine(
                                SearchEngineTypes.getByName(child.getId().replace("img", "")),
                                (boolean) child.getProperties().get("ENABLED")
                        )
                )
                .toList();
    }

    public static List<SearchEngine> getDefaultSearchEngineList() {
        return Arrays.stream(SearchEngineTypes.values())
                .map(searchEngineTypes -> new SearchEngine(searchEngineTypes, true))
                .toList();
    }
}
