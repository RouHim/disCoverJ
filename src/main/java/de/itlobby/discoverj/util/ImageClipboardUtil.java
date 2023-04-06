package de.itlobby.discoverj.util;

import de.itlobby.discoverj.models.TransferableImage;
import javafx.embed.swing.SwingFXUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Optional;

/**
 * Created by Rouven Himmelstein on 27.01.2016.
 */
public class ImageClipboardUtil implements ClipboardOwner {
    private static final Logger log = LogManager.getLogger(ImageClipboardUtil.class);

    public static Optional<BufferedImage> getImage() {
        try {
            // Get the system clipboard
            Clipboard clipboard = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();

            // Check if the clipboard content is an image
            if (clipboard.isDataFlavorAvailable(DataFlavor.imageFlavor)) {

                // Get the image data from the clipboard as a Transferable object
                Transferable transferable = clipboard.getContents(null);

                // Extract the image data as a BufferedImage
                BufferedImage bufferedImage = (BufferedImage) transferable.getTransferData(DataFlavor.imageFlavor);

                // return the image
                return Optional.of(bufferedImage);
            }
        } catch (IOException | UnsupportedFlavorException e) {
            log.error(e.getMessage(), e);
        }
        return Optional.empty();
    }

    public void setImage(javafx.scene.image.Image bi) {
        try {
            TransferableImage trans = new TransferableImage(SwingFXUtils.fromFXImage(bi, null));
            Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
            c.setContents(trans, this);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
        log.error("Lost ClipBrd ownership");
    }
}
