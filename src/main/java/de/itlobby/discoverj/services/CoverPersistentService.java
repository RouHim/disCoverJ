package de.itlobby.discoverj.services;

import de.itlobby.discoverj.models.AudioWrapper;
import de.itlobby.discoverj.util.ImageUtil;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class CoverPersistentService implements Service {
    private static final String PRE_DOWNLOAD_FOLDER = new File(System.getProperty("java.io.tmpdir"), "disCoverJ_tmp").getAbsolutePath();
    private static final Logger log = LogManager.getLogger(CoverPersistentService.class);

    public void persistImages(AudioWrapper audioWrapper, List<BufferedImage> images) {
        File targetFolder = new File(PRE_DOWNLOAD_FOLDER, audioWrapper.getIdentifier());

        if (!targetFolder.exists()) {
            targetFolder.mkdirs();
        }

        for (BufferedImage image : images) {
            File outputFile = new File(targetFolder.getAbsoluteFile(), image.hashCode() + ".jpg");
            BufferedImage rgbImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            rgbImage.createGraphics().drawImage(image, 0, 0, null);

            try {
                ImageIO.write(rgbImage, "jpg", outputFile);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    public List<BufferedImage> getCoversForAudioFile(AudioWrapper audioWrapper) {
        File targetFolder = new File(PRE_DOWNLOAD_FOLDER, audioWrapper.getIdentifier());
        File[] files = targetFolder.listFiles(pathname -> pathname.isFile() && pathname.getName().toLowerCase().endsWith(".jpg"));

        if (files == null) {
            return new ArrayList<>(0);
        }

        return Arrays.stream(files)
                .map(ImageUtil::readRGBImage)
                .flatMap(Optional::stream)
                .toList();
    }

    public void cleanup() {
        try {
            File file = new File(PRE_DOWNLOAD_FOLDER);
            if (file.exists()) {
                FileUtils.forceDelete(file);
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }
}
