package de.itlobby.discoverj.util;

import javafx.css.Styleable;

public class UIUtil {

    private UIUtil() {
        // static class
    }

    public static void addCSSClass(Styleable node, String... styleClasses) {
        for (String styleClass : styleClasses) {
            node.getStyleClass().add(styleClass);
        }
    }
}
