package de.itlobby.discoverj.ui.utils;

import de.jensd.fx.glyphs.GlyphIcons;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class AwesomeHelper {

  private AwesomeHelper() {}

  public static void setIconButton(
    Button button,
    FontAwesomeIcon faicon,
    double iconSize,
    String... additionalStyleClasses
  ) {
    FontAwesomeIconView iconView = new FontAwesomeIconView(faicon);
    for (String additionalStyleClas : additionalStyleClasses) {
      iconView.getStyleClass().add(additionalStyleClas);
    }
    iconView.setStyle(
      "-fx-font-family: FontAwesome; -fx-font-size: " + iconSize + "em;"
    );

    button.setGraphic(iconView);
  }

  public static void createIconButton(
    Button btn,
    GlyphIcons icon,
    String iconStyle,
    String tooltipText,
    String iconSize
  ) {
    Text iconTxt = GlyphsDude.createIcon(icon, iconSize);
    iconTxt.getStyleClass().add(iconStyle);
    btn.setGraphic(iconTxt);
    btn.setContentDisplay(ContentDisplay.CENTER);
    btn.getStyleClass().clear();
    btn.getStyleClass().add("font-button");
    btn.setTooltip(new Tooltip(tooltipText));
  }

  public static void createTextIcon(
    Label label,
    FontAwesomeIcon icon,
    String iconStyle,
    String tooltipText,
    String iconSize
  ) {
    Text iconTxt = GlyphsDude.createIcon(icon, iconSize);
    iconTxt.getStyleClass().add(iconStyle);
    label.setGraphic(iconTxt);
    label.setContentDisplay(ContentDisplay.CENTER);
    label.getStyleClass().clear();
    label.getStyleClass().add(iconStyle);
    label.setTooltip(new Tooltip(tooltipText));
  }

  public static void createCircleAnimation(
    Button btnActionCircle,
    Button btnActionCircleIcon,
    int fromValue,
    int toValue,
    int byY,
    int toY
  ) {
    Duration duration = Duration.millis(500);

    FadeTransition ft1 = new FadeTransition(duration, btnActionCircle);
    FadeTransition ft2 = new FadeTransition(duration, btnActionCircleIcon);
    ft1.setFromValue(fromValue);
    ft1.setToValue(toValue);
    ft2.setFromValue(fromValue);
    ft2.setToValue(toValue);

    TranslateTransition tt1 = new TranslateTransition(
      duration,
      btnActionCircle
    );
    TranslateTransition tt2 = new TranslateTransition(
      duration,
      btnActionCircleIcon
    );
    tt1.setByY(byY);
    tt1.setToY(toY);
    tt2.setByY(byY);
    tt2.setToY(toY);

    ft1.play();
    ft2.play();
    tt1.play();
    tt2.play();
  }
}
