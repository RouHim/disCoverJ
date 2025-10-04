package de.itlobby.discoverj.ui.viewcontroller;

import de.itlobby.discoverj.ui.utils.GlyphsDude;
import de.itlobby.discoverj.util.ImageUtil;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import java.awt.image.BufferedImage;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

public class ImageSelectionViewController implements ViewController {

    public VBox baseLayout;
    public VBox imgPropLayout;
    public CheckBox chkSquareImages;
    public CheckBox chkRemoveTopBottomBorder;
    public CheckBox chkRemoveLeftRightBorder;
    public FlowPane flowPane;
    public Button btnApply;
    public Text noImage;
    public ProgressIndicator progressIndicator;

    @Override
    public void initialize() {
        flowPane.prefWrapLengthProperty().bind(baseLayout.widthProperty().subtract(20));
        createNoImage();
    }

    public VBox buildCoverImageView(BufferedImage image) {
        double ratio = (double) image.getWidth() / (double) image.getHeight();

        int fitHeight = 250;
        double fitWidth = 250 * ratio;

        ImageView imageView = new ImageView(ImageUtil.toFXImage(image, (int) fitWidth, fitHeight));

        imageView.setFitHeight(fitHeight);
        imageView.setFitWidth(fitWidth);
        imageView.setCursor(Cursor.HAND);

        VBox layout = new VBox();
        layout.getStyleClass().add("light-box-bg");

        Label resInfo = new Label(String.format("(%sx%s)", image.getWidth(), image.getHeight()));
        resInfo.setStyle("-fx-text-alignment: center;");
        resInfo.setAlignment(Pos.CENTER);
        resInfo.setTextAlignment(TextAlignment.CENTER);
        resInfo.setPrefWidth(fitWidth);

        layout.getChildren().add(imageView);
        layout.getChildren().add(resInfo);

        VBox.setMargin(resInfo, new Insets(5, 0, 5, 0));
        return layout;
    }

    private void disableControls(boolean value) {
        chkSquareImages.setDisable(value);
        chkRemoveTopBottomBorder.setDisable(value);
        chkRemoveLeftRightBorder.setDisable(value);
        btnApply.setDisable(value);
    }

    public void setImagesToView(List<VBox> elements) {
        disableControls(false);

        flowPane.getChildren().clear();
        flowPane.getChildren().add(noImage);
        flowPane.getChildren().addAll(elements);
    }

    public void resetView() {
        disableControls(true);

        flowPane.getChildren().clear();

        progressIndicator = new ProgressIndicator();
        progressIndicator.setStyle("-fx-accent: #FF6F00; -fx-progress-color: #FF6F00;");
        progressIndicator.setPrefHeight(100);
        progressIndicator.setPrefWidth(100);

        flowPane.getChildren().add(progressIndicator);
    }

    private void createNoImage() {
        noImage = GlyphsDude.createIcon(FontAwesomeIcon.TIMES);
        noImage.getStyleClass().add("default-icon");
        noImage.setStyle("-fx-fill: red; -fx-font-family: FontAwesome; -fx-font-size: 128px;");
    }
}
