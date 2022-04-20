package de.itlobby.discoverj.services;

import de.itlobby.discoverj.ui.core.ServiceLocator;
import de.itlobby.discoverj.ui.core.ViewManager;
import de.itlobby.discoverj.ui.core.Views;
import de.itlobby.discoverj.listeners.ActionParamListener;
import de.itlobby.discoverj.util.ImageUtil;
import de.itlobby.discoverj.util.SystemUtil;
import de.itlobby.discoverj.ui.viewcontroller.ImageSelectionViewController;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import org.apache.commons.lang3.ObjectUtils;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ImageSelectionService implements Service {
    private LightBoxService lightBoxService;
    private List<BufferedImage> images;
    private ImageSelectionViewController viewController;
    private BufferedImage newCover = null;
    private boolean selectionRunning;

    public BufferedImage openImageSelection(List<BufferedImage> images, String title) {
        this.images = images;
        selectionRunning = true;

        sortAndDistinctImages();

        lightBoxService = ServiceLocator.get(LightBoxService.class);
        ViewManager viewManager = ViewManager.getInstance();
        Parent parent = viewManager.createLayoutFromView(Views.IMAGE_SELECTION_VIEW);
        viewController = viewManager.getViewController(Views.IMAGE_SELECTION_VIEW);

        viewController.btnApply.setOnAction(event -> applyFilter());
        viewController.noImage.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> onImageSelected(-1));

        reloadImagesToView();

        Platform.runLater(() ->
                lightBoxService.showContentAsDialog(title, parent, true, 0, 0)
        );

        while (selectionRunning) {
            SystemUtil.threadSleep(50);
        }

        return newCover;
    }

    private void applyFilter() {
        reloadImagesToView();
    }

    private void sortAndDistinctImages() {
        this.images = images.stream().distinct().sorted(
                (o1, o2) ->
                {
                    int m1 = o1.getHeight() * o1.getWidth();
                    int m2 = o2.getHeight() * o2.getWidth();
                    return ObjectUtils.compare(m1, m2) * -1;
                }
        ).collect(Collectors.toList());
    }

    private void reloadImagesToView() {
        viewController.resetView();

        ActionParamListener<List<VBox>> threadFinishedListener = elements -> Platform.runLater(() ->
                viewController.setImagesToView(elements)
        );

        Thread thread = new Thread(() -> threadFinishedListener.onAction(postProcessImagesAndGetNodes()));
        thread.setUncaughtExceptionHandler(ServiceLocator.get(ExceptionService.class));
        thread.start();
    }

    private List<VBox> postProcessImagesAndGetNodes() {
        List<VBox> imageViewList = new ArrayList<>();

        double imgCount = images.size();

        for (int i = 0; i < images.size(); i++) {
            final int finalI = i;
            Platform.runLater(() ->
                    viewController.progressIndicator.setProgress((finalI + 1) / imgCount)
            );

            BufferedImage image = images.get(i);

            if (viewController.chkRemoveTopBottomBorder.isSelected()) {
                image = ImageUtil.removeTopBottomBlackBorders(image);
            }
            if (viewController.chkRemoveLeftRightBorder.isSelected()) {
                image = ImageUtil.removeLeftRightBlackBorders(image);
            }
            if (viewController.chkSquareImages.isSelected()) {
                image = ImageUtil.squareImage(image);
            }

            images.set(i, image);

            VBox imageView = viewController.buildCoverImageView(image);
            imageView.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> onImageSelected(finalI));

            imageViewList.add(imageView);
        }

        return imageViewList;
    }

    private void onImageSelected(int index) {
        if (index >= 0) {
            newCover = images.get(index);
        }

        lightBoxService.hideDialog();
        selectionRunning = false;
    }
}
