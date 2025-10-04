package de.itlobby.discoverj.ui.utils;

import java.util.Arrays;
import javafx.css.Styleable;

public class UIUtil {

    private UIUtil() {
        // static class
    }

    public static void addCSSClass(Styleable node, String... styleClasses) {
        Arrays.stream(styleClasses).forEach(styleClass -> node.getStyleClass().add(styleClass));
    }
}
