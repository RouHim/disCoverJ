package de.itlobby.discoverj.util;

import de.itlobby.discoverj.models.SearchEngine;
import de.itlobby.discoverj.settings.Settings;
import javafx.animation.ScaleTransition;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Optional;

public class ImageUtil {
    private static final Logger log = LogManager.getLogger(ImageUtil.class);

    private ImageUtil() {
    }

    public static BufferedImage resize(BufferedImage inputImage, int scaledWidth, int scaledHeight) {
        BufferedImage outputImage = inputImage;

        try {
            outputImage = new BufferedImage(scaledWidth, scaledHeight, inputImage.getType());

            Graphics2D g2d = outputImage.createGraphics();
            g2d.drawImage(inputImage, 0, 0, scaledWidth, scaledHeight, null);
            g2d.dispose();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return outputImage;
    }

    public static byte[] toJpgByteArray(BufferedImage inputImage) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BufferedImage imageToWrite = inputImage;

            // If our input image is no valid INT_RGB PNG, convert it
            if (inputImage.getType() != BufferedImage.TYPE_INT_RGB) {
                imageToWrite = new BufferedImage(
                        inputImage.getWidth(),
                        inputImage.getHeight(),
                        BufferedImage.TYPE_INT_RGB
                );
                imageToWrite.createGraphics()
                        .drawImage(inputImage, 0, 0, java.awt.Color.WHITE, null);
            }

            ImageIO.write(imageToWrite, "jpg", outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }

        return null;
    }

    public static java.util.List<ImageView> createSearchEnginePictures(int fitSize, boolean initDragAndDrop, FlowPane parentHbox, boolean canEdit) {
        java.util.List<ImageView> imageViews = new ArrayList<>();

        for (SearchEngine searchEngine : Settings.getInstance().getConfig().getSearchEngineList()) {
            ImageView imageView = new ImageView(new Image(searchEngine.getType().getLogoPath()));
            imageView.setId("img" + searchEngine.getType().getName());
            imageViews.add(imageView);
            imageView.setFitHeight(fitSize);
            imageView.setFitWidth(fitSize);
            Tooltip.install(imageView, new Tooltip(searchEngine.getType().getName()));

            setSearchEngineImageViewEffect(imageView, searchEngine);
            imageView.getProperties().put("ENABLED", searchEngine.isEnabled());

            if (canEdit) {
                imageView.setCursor(javafx.scene.Cursor.HAND);
                imageView.setOnMouseClicked(event -> toogleDisabled(imageView, searchEngine));
            }

            HBox.setMargin(imageView, new javafx.geometry.Insets(0, 5, 0, 0));

            if (initDragAndDrop) {
                initDragAndDrop(imageView, parentHbox);
            }
        }

        return imageViews;
    }

    public static void toogleDisabled(ImageView imageView, SearchEngine searchEngine) {
        boolean isEnabled = !searchEngine.isEnabled();

        searchEngine.setEnabled(isEnabled);
        imageView.getProperties().put("ENABLED", isEnabled);
        setSearchEngineImageViewEffect(imageView, searchEngine);
    }

    public static void setSearchEngineImageViewEffect(ImageView imageView, SearchEngine searchEngine) {
        if (searchEngine.isEnabled()) {
            imageView.setEffect(null);
        } else {
            ColorAdjust colorAdjust = new ColorAdjust();
            colorAdjust.setBrightness(-0.5);
            colorAdjust.setSaturation(-1);
            imageView.setEffect(colorAdjust);
        }
    }

    private static void initDragAndDrop(ImageView imageView, FlowPane flowPane) {
        imageView.setOnDragDetected(
                event ->
                {
                    Dragboard db = imageView.startDragAndDrop(TransferMode.ANY);

                    ClipboardContent content = new ClipboardContent();
                    content.putString(imageView.getId());
                    db.setContent(content);

                    event.consume();
                }
        );

        imageView.setOnDragOver(
                event ->
                {

                    if (event.getDragboard().hasString() && !imageView.getId().equals(event.getDragboard().getString())) {
                        event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                    }

                    event.consume();
                }
        );

        imageView.setOnDragEntered(
                event ->
                {

                    if (event.getDragboard().hasString() && !imageView.getId().equals(event.getDragboard().getString())) {
                        ScaleTransition st = new ScaleTransition(Duration.millis(200), imageView);
                        st.setFromX(1);
                        st.setToX(1.2);
                        st.setFromY(1);
                        st.setToY(1.2);
                        st.setCycleCount(1);
                        st.setAutoReverse(false);
                        st.play();
                    }

                    event.consume();
                }
        );

        imageView.setOnDragExited(
                event ->
                {

                    if (event.getDragboard().hasString() && !imageView.getId().equals(event.getDragboard().getString())) {
                        ScaleTransition st = new ScaleTransition(Duration.millis(200), imageView);
                        st.setFromX(1.2);
                        st.setToX(1);
                        st.setFromY(1.2);
                        st.setToY(1);
                        st.setCycleCount(1);
                        st.setAutoReverse(false);
                        st.play();
                    }

                    event.consume();
                }
        );

        imageView.setOnDragDropped(
                event ->
                {
                    Dragboard db = event.getDragboard();
                    boolean success = false;

                    if (db.hasString()) {
                        ImageView src = (ImageView) SystemUtil.getChildrenById(flowPane, db.getString());

                        flowPane.getChildren().remove(src);
                        flowPane.getChildren().add(flowPane.getChildren().indexOf(imageView), src);

                        success = true;
                    }
                    event.setDropCompleted(success);

                    event.consume();
                }
        );

        imageView.setOnDragDone(
                javafx.event.Event::consume
        );
    }

    public static String colorToHexString(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255)
        );
    }

    public static String createImageResolutionString(double width, double height) {
        return String.format("(%sx%s)", (int) width, (int) height);
    }

    public static BufferedImage removeTopBottomBlackBorders(BufferedImage input) {
        BufferedImage output = input;

        try {
            ArrayList<Integer> lineAvg = new ArrayList<>();

            for (int y = 0; y < input.getHeight(); y++) {
                int sumForLine = 0;

                for (int x = 0; x < input.getWidth(); x++) {
                    int clr = input.getRGB(x, y);
                    int red = (clr & 0x00ff0000) >> 16;
                    int green = (clr & 0x0000ff00) >> 8;
                    int blue = clr & 0x000000ff;

                    sumForLine += (red + green + blue) / 3;
                }

                lineAvg.add(sumForLine / input.getWidth());
            }

            int firstFilledLine = 0;
            int lastFilledLine = 0;
            int treshold = 5;

            for (int i = 0; i < lineAvg.size(); i++) {
                Integer lineVal = lineAvg.get(i);
                if (lineVal >= treshold) {
                    firstFilledLine = i;
                    break;
                }
            }

            for (int i = lineAvg.size() - 1; i >= 0; i--) {
                Integer lineVal = lineAvg.get(i);
                if (lineVal >= treshold) {
                    lastFilledLine = i;
                    break;
                }
            }

            int newHeight = input.getHeight() - (firstFilledLine) - (input.getHeight() - (lastFilledLine + 1));

            output = new BufferedImage(input.getWidth(), newHeight, input.getType());

            for (int y = 0; y < output.getHeight(); y++) {
                for (int x = 0; x < output.getWidth(); x++) {
                    output.setRGB(x, y, input.getRGB(x, y + firstFilledLine));
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return output;
    }

    public static BufferedImage removeLeftRightBlackBorders(BufferedImage input) {
        BufferedImage output = input;
        try {
            ArrayList<Integer> columnAvg = new ArrayList<>();

            for (int x = 0; x < input.getWidth(); x++) {
                int sumForColumn = 0;

                for (int y = 0; y < input.getHeight(); y++) {
                    int clr = input.getRGB(x, y);
                    int red = (clr & 0x00ff0000) >> 16;
                    int green = (clr & 0x0000ff00) >> 8;
                    int blue = clr & 0x000000ff;

                    sumForColumn += (red + green + blue) / 3;
                }

                columnAvg.add(sumForColumn / input.getWidth());
            }

            int firstFilledColumn = 0;
            int lastFilledColumn = 0;
            int treshold = 5;

            for (int i = 0; i < columnAvg.size(); i++) {
                Integer colVal = columnAvg.get(i);
                if (colVal >= treshold) {
                    firstFilledColumn = i;
                    break;
                }
            }

            for (int i = columnAvg.size() - 1; i >= 0; i--) {
                Integer colVal = columnAvg.get(i);
                if (colVal >= treshold) {
                    lastFilledColumn = i;
                    break;
                }
            }

            int newWidth = input.getWidth() - (firstFilledColumn) - (input.getWidth() - (lastFilledColumn + 1));
            output = new BufferedImage(newWidth, input.getHeight(), input.getType());

            for (int x = 0; x < output.getWidth(); x++) {
                for (int y = 0; y < output.getHeight(); y++) {
                    output.setRGB(x, y, input.getRGB(x + firstFilledColumn, y));
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return output;
    }

    public static BufferedImage squareImage(BufferedImage input) {
        BufferedImage squaredImage = input;
        int height = input.getHeight();
        int width = input.getWidth();

        if (height != width) {
            int targetSize = height > width ? width : height;
            squaredImage = ImageUtil.resize(input, targetSize, targetSize);
        }

        return squaredImage;
    }

    public static Optional<BufferedImage> readRGBImageFromUrl(String imgUrl) {
        try {
            BufferedImage urlImage = ImageIO.read(new URL(imgUrl));
            if (urlImage == null) {
                return Optional.empty();
            }

            BufferedImage rgbImage = new BufferedImage(urlImage.getWidth(), urlImage.getHeight(), BufferedImage.TYPE_INT_RGB);
            rgbImage.createGraphics().drawImage(urlImage, 0, 0, null);
            return Optional.of(rgbImage);
        } catch (IOException e) {
            log.error("{} from url {}", e.getMessage(), imgUrl);
        }

        return Optional.empty();
    }

    public static Image resize(Image inputImage, int targetWidth, int scaledHeight) {
        WritableImage writableImage = null;

        if (inputImage != null) {
            writableImage = new WritableImage(targetWidth, scaledHeight);
            PixelWriter pixelWriter = writableImage.getPixelWriter();
            PixelReader pixelReader = inputImage.getPixelReader();

            int originalWidth = (int) inputImage.getWidth();
            int originalHeight = (int) inputImage.getHeight();

            double sWidth = originalWidth / (double) targetWidth;
            double sHeight = originalHeight / (double) scaledHeight;

            for (int x = 0; x < targetWidth; x++) {

                int xF = (int) (x * sWidth);

                for (int y = 0; y < scaledHeight; y++) {
                    int yF = (int) (y * sHeight);

                    int rgbAtXfYf = pixelReader.getArgb(xF, yF);
                    pixelWriter.setArgb(x, y, rgbAtXfYf);
                }
            }
        }

        return writableImage;
    }

    public static WritableImage toFXImage(BufferedImage image, int targetWidth, int targetHeight) {
        WritableImage writableImage = null;

        if (image != null) {
            writableImage = new WritableImage(targetWidth, targetHeight);
            PixelWriter pixelWriter = writableImage.getPixelWriter();

            int originalWidth = image.getWidth();
            int originalHeight = image.getHeight();

            double sWidth = originalWidth / (double) targetWidth;
            double sHeight = originalHeight / (double) targetHeight;

            for (int x = 0; x < targetWidth; x++) {

                int xF = (int) (x * sWidth);

                for (int y = 0; y < targetHeight; y++) {
                    int yF = (int) (y * sHeight);

                    int rgbAtXfYf = image.getRGB(xF, yF);
                    pixelWriter.setArgb(x, y, rgbAtXfYf);
                }
            }
        }

        return writableImage;
    }

    public static BufferedImage fixColorModel(BufferedImage src) {
        BufferedImage img = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_BGR);
        Graphics2D g2d = img.createGraphics();
        g2d.drawImage(src, 0, 0, null);
        g2d.dispose();
        return img;
    }

    public static Optional<Image> getFxImageFromBytes(byte[] data, int width, int height) {
        if (data == null || data.length <= 0) {
            return Optional.empty();
        }

        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
            BufferedImage bufferedImage = ImageIO.read(inputStream);

            BufferedImage rgbImage = new BufferedImage(bufferedImage.getWidth(), bufferedImage.getHeight(), BufferedImage.TYPE_INT_RGB);
            rgbImage.createGraphics().drawImage(bufferedImage, 0, 0, null);

            return Optional.ofNullable(toFXImage(rgbImage, width, height));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return Optional.empty();
    }

    public static Optional<Image> getFxImageFromBytes(byte[] data) {
        if (data == null || data.length <= 0) {
            return Optional.empty();
        }

        try {
            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(data));

            BufferedImage rgbImage = new BufferedImage(bufferedImage.getWidth(), bufferedImage.getHeight(), BufferedImage.TYPE_INT_RGB);
            rgbImage.createGraphics().drawImage(bufferedImage, 0, 0, null);

            return Optional.ofNullable(toFXImage(rgbImage, rgbImage.getWidth(), rgbImage.getHeight()));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return Optional.empty();
    }

    /**
     * Reads an image as RGB from a file.
     *
     * @param file to read
     * @return the read rgb image
     */
    public static Optional<BufferedImage> readRGBImage(File file) {
        try {
            BufferedImage read = ImageIO.read(file);
            BufferedImage rgbImage = new BufferedImage(read.getWidth(), read.getHeight(), BufferedImage.TYPE_INT_RGB);
            rgbImage.createGraphics().drawImage(read, 0, 0, null);
            return Optional.of(rgbImage);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }

        return Optional.empty();
    }
}
