package de.itlobby.discoverj.ui.viewcontroller;

import de.itlobby.discoverj.models.FlatAudioWrapper;
import de.itlobby.discoverj.ui.core.ViewManager;
import de.itlobby.discoverj.ui.core.Views;
import de.itlobby.discoverj.util.ImageUtil;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.Optional;

public class CoverDetailViewController implements ViewController {
    public TextFlow txtCoverInfo;
    public ImageView imgCover;
    public HBox layoutCoverImage;

    @Override
    public void initialize() {
        MainViewController viewController = ViewManager.getInstance().getViewController(Views.MAIN_VIEW);

        viewController.lightBoxLayout.heightProperty().addListener((observable, oldValue, newValue) ->
                onSizeChanged(viewController.lightBoxLayout)
        );
    }

    private void onSizeChanged(HBox lightBoxLayout) {
        fitImageSizeImage(lightBoxLayout);
    }

    public void fitImageSizeImage(HBox lightBoxHbox) {
        if (imgCover.getImage() != null) {
            double maxHeight = lightBoxHbox.getHeight();
            double maxWidth = lightBoxHbox.getWidth();

            double imgHeight = imgCover.getImage().getHeight() + 150;
            double imgWidth = imgCover.getImage().getWidth() - 50;

            if (imgHeight > maxHeight || imgWidth > maxWidth) {
                imgCover.setFitHeight(maxHeight - 150);
                imgCover.setFitWidth(maxWidth - 50);
            } else {
                imgCover.setFitHeight(imgHeight);
                imgCover.setFitWidth(imgWidth);
            }
        }
    }

    public void createCoverInfo(FlatAudioWrapper wrapper) {
        Optional<Image> maybeImage = wrapper.getImage();

        if (wrapper.isHasCover() && maybeImage.isPresent()) {
            Image image = maybeImage.get();
            txtCoverInfo.getChildren().add(
                    new Text(
                            ImageUtil.createImageResolutionString(image.getWidth(), image.getHeight()))
            );
        } else {
            txtCoverInfo.getChildren().clear();
        }
    }
}