package de.itlobby.discoverj.util.helper;

import de.itlobby.discoverj.util.ImageUtil;
import javafx.scene.image.Image;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ImageCache {
    private static final Logger LOG = LogManager.getLogger(ImageCache.class);
    private static final int MAX_CACHE_ENTRIES = Math.min(
            (int) (Runtime.getRuntime().maxMemory() / (1024 * 1024) / (7 * 3)), // check how big the ram is
            200 // max 200 entries
    );
    private static final Map<Integer, Image> fxImageCache = new ConcurrentHashMap<>();
    private static final Map<Integer, BufferedImage> buffImageCache = new ConcurrentHashMap<>();
    private static ImageCache instance;

    private ImageCache() {
    }

    public static ImageCache getInstance() {
        if (instance == null) {
            instance = new ImageCache();
        }

        return instance;
    }

    public void clear() {
        fxImageCache.clear();
        buffImageCache.clear();
    }

    public Optional<Image> getImage(byte[] data) {
        int hashCode = Arrays.hashCode(data);

        Optional<Image> maybeImage = Optional.ofNullable(fxImageCache.get(hashCode));

        if (maybeImage.isEmpty()) {
            maybeImage = ImageUtil.getFxImageFromBytes(data);
            maybeImage.ifPresent(image -> cacheImage(hashCode, image));
        }

        return maybeImage;
    }

    public Optional<Image> getImage(byte[] data, int width, int height) {
        if (data == null || data.length <= 0) {
            return Optional.empty();
        }

        int hashCode = Arrays.hashCode(data) + width + height;

        Optional<Image> image;

        if (fxImageCache.containsKey(hashCode)) {
            image = Optional.ofNullable(fxImageCache.get(hashCode));
        } else {
            try {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                Thumbnails.of(new ByteArrayInputStream(data))
                        .size(width, height)
                        .determineOutputFormat()
                        .toOutputStream(os);
                image = ImageUtil.getFxImageFromBytes(os.toByteArray());
            } catch (IOException e) {
                image = ImageUtil.getFxImageFromBytes(data, width, height);
            }
        }

        image.ifPresent(i -> cacheImage(hashCode, i));

        return image;
    }

    private void cacheImage(int hashCode, Image image) {
        if (fxImageCache.size() >= MAX_CACHE_ENTRIES) {
            Iterator<Integer> iterator = fxImageCache.keySet().iterator();
            Integer firstKEy = iterator.next();
            fxImageCache.remove(firstKEy);
        }

        fxImageCache.put(hashCode, image);
    }

    public BufferedImage getBuffImage(byte[] data) {
        try {
            int hashCode = Arrays.hashCode(data);

            if (buffImageCache.containsKey(hashCode)) {
                return buffImageCache.get(hashCode);
            } else {
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
                buffImageCache.put(hashCode, image);
                return image;
            }
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }

        return null;
    }
}
