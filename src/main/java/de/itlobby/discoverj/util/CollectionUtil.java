package de.itlobby.discoverj.util;

import de.itlobby.discoverj.models.SearchEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by Rouven Himmelstein on 02.08.2016.
 */
public class CollectionUtil {
    private CollectionUtil() {
        // hide constructor
    }

    public static <K, V extends Comparable<? super V>> Map.Entry<K, V> sortByValueDescAndGetFirst(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new LinkedList<>(map.entrySet());
        Collections.sort(list, (o1, o2) -> (o1.getValue()).compareTo(o2.getValue()));
        Collections.reverse(list);
        return list.get(0);
    }

    public static List<SearchEngine> cloneList(List<SearchEngine> list) {
        List<SearchEngine> clone = new ArrayList<>(list.size());
        for (SearchEngine item : list) {
            clone.add(item.cloneSearchEngine());
        }
        return clone;
    }
}
