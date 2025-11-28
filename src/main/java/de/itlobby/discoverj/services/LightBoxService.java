package de.itlobby.discoverj.services;

import de.itlobby.discoverj.listeners.ActionListener;
import de.itlobby.discoverj.ui.utils.UIUtil;
import de.itlobby.discoverj.ui.viewcontroller.MainViewController;
import de.itlobby.discoverj.util.LanguageUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;

public class LightBoxService implements Service {

  private static final int DEFAULT_HEIGHT = 250;
  private static final int DEFAULT_WIDTH = 350;

  public void showDialog(
    String title,
    Parent content,
    ActionListener cancelListener,
    ActionListener okListener,
    boolean isFullscreen,
    boolean hideButtons
  ) {
    showInternal(
      title,
      content,
      cancelListener,
      okListener,
      isFullscreen,
      hideButtons
    );
  }

  public void showDialog(
    String title,
    Parent content,
    ActionListener cancelListener,
    ActionListener okListener,
    boolean hideButtons
  ) {
    showInternal(
      title,
      content,
      cancelListener,
      okListener,
      false,
      hideButtons
    );
  }

  private void showInternal(
    String title,
    Parent content,
    ActionListener cancelListener,
    ActionListener okListener,
    boolean isFullscreen,
    boolean hideButtons
  ) {
    if (okListener == null && cancelListener == null) {
      cancelListener = this::hideDialog;
    }

    if (hideButtons) {
      cancelListener = null;
      okListener = null;
    }

    final ActionListener finalCancelListener = cancelListener;
    final ActionListener finalOkListener = okListener;

    HBox buttonLayout = new HBox();
    buttonLayout.setAlignment(Pos.BOTTOM_CENTER);

    Button cancelButton = new Button();
    cancelButton.setOnAction(event -> finalCancelListener.onAction());
    cancelButton.setCancelButton(true);
    cancelButton.setText(LanguageUtil.getString("Dialog.close"));
    UIUtil.addCSSClass(cancelButton, "dialog-button");

    Button okButton = new Button();
    okButton.setOnAction(event -> finalOkListener.onAction());
    okButton.setDefaultButton(true);
    okButton.setText(LanguageUtil.getString("Dialog.ok"));
    UIUtil.addCSSClass(okButton, "dialog-button");

    if (cancelListener != null) {
      buttonLayout.getChildren().add(cancelButton);
    }
    if (okListener != null) {
      buttonLayout.getChildren().add(okButton);
    }

    HBox.setHgrow(okButton, Priority.ALWAYS);
    HBox.setHgrow(cancelButton, Priority.ALWAYS);

    int btnCount = buttonLayout.getChildren().size();
    okButton
      .prefWidthProperty()
      .bind(buttonLayout.widthProperty().divide(btnCount));
    cancelButton
      .prefWidthProperty()
      .bind(buttonLayout.widthProperty().divide(btnCount));

    VBox contentRootBox = new VBox();
    contentRootBox.setStyle("-fx-background-color: transparent");
    contentRootBox.getChildren().add(content);
    contentRootBox.getChildren().add(buttonLayout);

    VBox.setVgrow(buttonLayout, Priority.NEVER);
    VBox.setVgrow(content, Priority.ALWAYS);

    VBox layoutBox = new VBox();
    UIUtil.addCSSClass(layoutBox, "light-box-bg");
    Text titleText = new Text(title);
    titleText.setFill(Color.WHITE);

    TextFlow tfTitle = new TextFlow(titleText);
    tfTitle.setMinHeight(40);
    tfTitle.setTextAlignment(TextAlignment.CENTER);
    tfTitle.setPadding(new Insets(10, 0, 0, 0));

    UIUtil.addCSSClass(tfTitle, "light-box-title");
    layoutBox.getChildren().add(tfTitle);
    layoutBox.getChildren().add(contentRootBox);

    VBox.setVgrow(tfTitle, Priority.NEVER);
    VBox.setVgrow(contentRootBox, Priority.ALWAYS);

    layoutBox.setVisible(true);

    MainViewController viewController = getMainViewController();

    if (isFullscreen) {
      layoutBox
        .minHeightProperty()
        .bind(viewController.lightBoxLayout.heightProperty());
      layoutBox
        .minWidthProperty()
        .bind(viewController.lightBoxLayout.widthProperty());
    } else {
      layoutBox.setMaxHeight(DEFAULT_HEIGHT);
      layoutBox.setMaxWidth(DEFAULT_WIDTH);
    }

    viewController.lightBoxLayout.setVisible(true);
    viewController.lightBoxLayout.getChildren().clear();
    viewController.lightBoxLayout.getChildren().add(layoutBox);
  }

  public void hideDialog() {
    MainViewController viewController = getMainViewController();
    viewController.lightBoxLayout.setVisible(false);
    viewController.lightBoxLayout.getChildren().clear();
  }

  public void showTextDialog(String title, String message) {
    TextFlow textFlow = new TextFlow(new Text(message));
    textFlow.setPadding(new Insets(5));
    showDialog(title, textFlow, null, null, false);
  }

  public void showTextDialog(
    String title,
    String message,
    boolean hideButtons
  ) {
    TextFlow textFlow = new TextFlow(new Text(message));
    textFlow.setPadding(new Insets(5));
    showDialog(title, textFlow, null, null, hideButtons);
  }

  public void showContentAsDialog(
    String title,
    Parent content,
    boolean isFullscreen,
    int height,
    int width
  ) {
    VBox contentRootBox = new VBox(10);
    contentRootBox.setStyle("-fx-background-color: transparent;");
    contentRootBox.getChildren().add(content);

    VBox.setVgrow(content, Priority.ALWAYS);

    VBox layoutBox = new VBox(10);
    UIUtil.addCSSClass(layoutBox, "light-box-bg");
    Text titleText = new Text(title);
    titleText.setFill(Color.WHITE);
    TextFlow tfTitle = new TextFlow(titleText);
    tfTitle.setPadding(new Insets(5, 0, 5, 0));

    UIUtil.addCSSClass(tfTitle, "light-box-title");
    layoutBox.getChildren().add(tfTitle);
    layoutBox.getChildren().add(contentRootBox);

    VBox.setVgrow(titleText, Priority.NEVER);
    VBox.setVgrow(contentRootBox, Priority.ALWAYS);

    layoutBox.setVisible(true);

    if (!isFullscreen) {
      layoutBox.setMaxHeight(height);
      layoutBox.setMaxWidth(width);
    }

    getMainViewController().lightBoxLayout.setVisible(true);
    getMainViewController().lightBoxLayout.getChildren().clear();
    getMainViewController().lightBoxLayout.getChildren().add(layoutBox);

    BorderPane.setMargin(layoutBox, new Insets(5));

    if (isFullscreen) {
      HBox.setHgrow(layoutBox, Priority.ALWAYS);
    }
  }
}
