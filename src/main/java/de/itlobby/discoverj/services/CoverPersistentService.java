package de.itlobby.discoverj.services;

import de.itlobby.discoverj.models.AudioWrapper;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class CoverPersistentService implements Service {
    private static final String PRE_DOWNLOAD_FOLDER = Paths.get(System.getProperty("java.io.tmpdir"), "disCoverJ_tmp").toFile().getAbsolutePath();
    private static final Logger log = LogManager.getLogger(CoverPersistentService.class);

    public void persistImages(AudioWrapper audioWrapper, List<BufferedImage> images) {
        File targetFolder = new File(String.format("%s/%s",
                PRE_DOWNLOAD_FOLDER, audioWrapper.getIdentifier()));

        if (!targetFolder.exists()) {
            targetFolder.mkdirs();
        }

        for (BufferedImage image : images) {
            try {
                File outputFile = Paths.get(String.format("%s/%s.jpg",
                        targetFolder.getAbsoluteFile(), image.hashCode())).toFile();
                ImageIO.write(image, "jpg", outputFile);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    public List<BufferedImage> getCoversForAudioFile(AudioWrapper audioWrapper) {
        ArrayList<BufferedImage> bufferedImages = new ArrayList<>();

        File targetFolder = new File(String.format("%s/%s",
                PRE_DOWNLOAD_FOLDER, audioWrapper.getIdentifier()));

        File[] files = targetFolder.listFiles(pathname -> pathname.isFile() && pathname.getName()
                .toLowerCase().endsWith(".jpg"));

        if (files != null) {
            for (File file : files) {
                try {
                    bufferedImages.add(ImageIO.read(file));
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }

        return bufferedImages;
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
