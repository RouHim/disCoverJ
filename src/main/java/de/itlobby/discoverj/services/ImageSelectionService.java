package de.itlobby.discoverj.services;

import de.itlobby.discoverj.listeners.ActionParamListener;
import de.itlobby.discoverj.models.ImageFile;
import de.itlobby.discoverj.ui.core.ServiceLocator;
import de.itlobby.discoverj.ui.core.ViewManager;
import de.itlobby.discoverj.ui.core.Views;
import de.itlobby.discoverj.ui.viewcontroller.ImageSelectionViewController;
import de.itlobby.discoverj.util.ImageUtil;
import de.itlobby.discoverj.util.SystemUtil;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javax.imageio.ImageIO;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ImageSelectionService implements Service {

  private static final Logger log = LogManager.getLogger(
    ImageSelectionService.class
  );
  private LightBoxService lightBoxService;
  private List<ImageFile> imageFiles;
  private ImageSelectionViewController viewController;
  private Optional<ImageFile> selectedCover = Optional.empty();
  private boolean selectionRunning;

  public Optional<ImageFile> openImageSelection(
    List<ImageFile> images,
    String title
  ) {
    this.imageFiles = images;
    selectionRunning = true;

    sortAndDistinctImages();

    lightBoxService = ServiceLocator.get(LightBoxService.class);
    ViewManager viewManager = ViewManager.getInstance();
    Parent parent = viewManager.createLayoutFromView(
      Views.IMAGE_SELECTION_VIEW
    );
    viewController = viewManager.getViewController(Views.IMAGE_SELECTION_VIEW);

    viewController.btnApply.setOnAction(event -> applyFilter());
    viewController.noImage.addEventFilter(MouseEvent.MOUSE_CLICKED, event ->
      onImageSelected(-1)
    );

    reloadImagesToView();

    Platform.runLater(() ->
      lightBoxService.showContentAsDialog(title, parent, true, 0, 0)
    );

    while (selectionRunning) {
      SystemUtil.threadSleep(50);
    }

    return selectedCover;
  }

  private void applyFilter() {
    reloadImagesToView();
  }

  private void sortAndDistinctImages() {
    this.imageFiles = imageFiles
      .stream()
      .distinct()
      .sorted((o1, o2) -> {
        int m1 = o1.height() * o1.width();
        int m2 = o2.height() * o2.width();
        return ObjectUtils.compare(m1, m2) * -1;
      })
      .toList();
  }

  private void reloadImagesToView() {
    viewController.resetView();

    ActionParamListener<List<VBox>> threadFinishedListener = elements ->
      Platform.runLater(() -> viewController.setImagesToView(elements));

    Thread.ofVirtual()
      .uncaughtExceptionHandler(ServiceLocator.get(ExceptionService.class))
      .start(() ->
        threadFinishedListener.onAction(postProcessImagesAndGetNodes())
      );
  }

  private List<VBox> postProcessImagesAndGetNodes() {
    List<VBox> imageViewList = new ArrayList<>();

    double imgCount = imageFiles.size();

    for (int i = 0; i < imageFiles.size(); i++) {
      final int finalI = i;
      Platform.runLater(() ->
        viewController.progressIndicator.setProgress((finalI + 1) / imgCount)
      );

      Optional<BufferedImage> image = readAndTransformImage(imageFiles.get(i));
      if (image.isEmpty()) {
        continue;
      }

      VBox imageView = viewController.buildCoverImageView(image.get());
      imageView.addEventFilter(MouseEvent.MOUSE_CLICKED, event ->
        onImageSelected(finalI)
      );

      imageViewList.add(imageView);
    }

    return imageViewList;
  }

  private Optional<BufferedImage> readAndTransformImage(ImageFile imageFile) {
    try {
      BufferedImage bufferImage;

      bufferImage = ImageIO.read(new File(imageFile.filePath()));

      if (viewController.chkRemoveTopBottomBorder.isSelected()) {
        bufferImage = ImageUtil.removeTopBottomBlackBorders(bufferImage);
      }
      if (viewController.chkRemoveLeftRightBorder.isSelected()) {
        bufferImage = ImageUtil.removeLeftRightBlackBorders(bufferImage);
      }
      if (viewController.chkSquareImages.isSelected()) {
        bufferImage = ImageUtil.squareImage(bufferImage);
      }
      return Optional.ofNullable(bufferImage);
    } catch (IOException e) {
      log.error(e.getMessage(), e);
    }

    return Optional.empty();
  }

  private void onImageSelected(int index) {
    try {
      if (index >= 0) {
        Optional<BufferedImage> image = readAndTransformImage(
          imageFiles.get(index)
        );

        // Write transformed image back to the file
        if (image.isPresent()) {
          ImageFile imageFile = imageFiles.get(index);
          ImageIO.write(image.get(), "jpg", new File(imageFile.filePath()));
          selectedCover = Optional.of(imageFile);
        }
      }
    } catch (IOException e) {
      log.error(e.getMessage(), e);
      selectedCover = Optional.empty();
    }

    lightBoxService.hideDialog();
    selectionRunning = false;
  }
}
