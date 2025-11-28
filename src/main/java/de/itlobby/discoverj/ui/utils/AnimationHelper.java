package de.itlobby.discoverj.ui.utils;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * Created by Rouven on 17.09.2016.
 */
public class AnimationHelper {

    private AnimationHelper() {
    }

    public static void slide(Node node, int fromValue, int toValue, int by, int to) {
        Duration duration = Duration.millis(500);

        FadeTransition ft1 = new FadeTransition(duration, node);
        ft1.setFromValue(fromValue);
        ft1.setToValue(toValue);

        TranslateTransition tt1 = new TranslateTransition(duration, node);
        tt1.setByX(by);
        tt1.setToX(to);

        ft1.play();
        tt1.play();
    }
}
