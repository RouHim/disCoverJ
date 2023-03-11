package de.itlobby.discoverj.ui.components;

import de.itlobby.discoverj.models.AudioWrapper;
import de.itlobby.discoverj.services.InitialService;
import de.itlobby.discoverj.settings.Settings;
import de.itlobby.discoverj.ui.core.ServiceLocator;
import de.itlobby.discoverj.util.ImageUtil;
import de.itlobby.discoverj.util.LanguageUtil;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.animation.Animation;
import javafx.animation.RotateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.Duration;

import java.awt.image.BufferedImage;
import java.util.Objects;

public class AudioListEntry extends HBox {
    private final AudioWrapper audioWrapper;
    private ImageView imageView;
    private FontAwesomeIconView iconView;
    private Label txtPath;
    private RotateTransition rotateTransition;

    public AudioListEntry(AudioWrapper audioWrapper) {
        this.audioWrapper = audioWrapper;
        createLayout();
    }

    private void createLayout() {
        txtPath = new Label(audioWrapper.getDisplayValue());
        iconView = new FontAwesomeIconView();

        updateStatusIcon();

        iconView.setStyle("-fx-font-family: FontAwesome; -fx-font-size: 2em; -fx-text-alignment: center;");
        iconView.setWrappingWidth(36);

        if (!Settings.getInstance().isCoverLoadingDisabled()) {
            imageView = new ImageView();
            imageView.setFitHeight(36);
            imageView.setFitWidth(36);
            imageView.setSmooth(true);
            imageView.setCache(false);
            getChildren().add(imageView);
            HBox.setHgrow(imageView, Priority.NEVER);
        }

        HBox labelLayout = new HBox(txtPath);

        getChildren().addAll(labelLayout, iconView);

        setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(labelLayout, Priority.ALWAYS);
        HBox.setHgrow(iconView, Priority.NEVER);
        HBox.setMargin(labelLayout, new Insets(5));

        setSpacing(5);
        getStyleClass().add("audio-line");

        setOnMouseClicked(event ->
        {
            if (event.getButton() == MouseButton.PRIMARY) {
                ServiceLocator.get(InitialService.class)
                        .selectLine(AudioListEntry.this, event.isControlDown(), event.isShiftDown());
            }
        });

        initAnimation();
    }

    public AudioWrapper getWrapper() {
        return audioWrapper;
    }

    public ImageView getImageView() {
        return imageView;
    }

    public FontAwesomeIconView getIconView() {
        return iconView;
    }

    public void removeCover() {
        if (imageView != null) {
            imageView.setImage(null);
        }
        iconView.setIcon(FontAwesomeIcon.TIMES);
        audioWrapper.setHasCover(false);
    }

    public void replaceCover(BufferedImage img) {
        WritableImage fxImg = ImageUtil.toFXImage(img, (int) imageView.getFitWidth(), (int) imageView.getFitHeight());
        imageView.setImage(fxImg);
        iconView.setIcon(FontAwesomeIcon.CHECK);
        audioWrapper.setHasCover(true);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AudioListEntry that = (AudioListEntry) o;

        if (!Objects.equals(audioWrapper, that.getWrapper())) {
            return false;
        }

        return audioWrapper.equals(((AudioListEntry) o).getWrapper());
    }

    @Override
    public int hashCode() {
        int result = audioWrapper != null ? audioWrapper.hashCode() : 0;
        result = 31 * result + (imageView != null ? imageView.hashCode() : 0);
        result = 31 * result + (iconView != null ? iconView.hashCode() : 0);
        result = 31 * result + (txtPath != null ? txtPath.hashCode() : 0);
        return result;
    }

    public void setBusy(boolean isBusy) {
        if (isBusy) {
            iconView.setIcon(FontAwesomeIcon.SPINNER);
            startRotate();
        } else {
            stopRotate();
            updateStatusIcon();
        }
    }

    private void updateStatusIcon() {
        FontAwesomeIcon icon;

        if (audioWrapper.hasCover()) {
            icon = FontAwesomeIcon.CHECK;
        } else {
            icon = FontAwesomeIcon.TIMES;
        }
        if (audioWrapper.isReadOnly()) {
            icon = FontAwesomeIcon.LOCK;
            Tooltip.install(iconView, new Tooltip(LanguageUtil.getString("audiofile.readonly")));
        }

        iconView.setIcon(icon);
    }

    private void initAnimation() {
        rotateTransition = new RotateTransition(Duration.millis(2000), iconView);
        rotateTransition.setFromAngle(0);
        rotateTransition.setToAngle(360);
        rotateTransition.setCycleCount(Animation.INDEFINITE);
        rotateTransition.setAutoReverse(false);
        rotateTransition.statusProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == Animation.Status.STOPPED) {
                RotateTransition transition = new RotateTransition(Duration.millis(10), iconView);
                transition.setFromAngle(iconView.getRotate());
                transition.setToAngle(0);
                transition.setCycleCount(1);
                transition.setAutoReverse(true);
                transition.play();
            }
        });
    }

    private void startRotate() {
        if (rotateTransition.getStatus() != Animation.Status.RUNNING) {
            rotateTransition.play();
        }
    }

    private void stopRotate() {
        rotateTransition.stop();
    }
}
