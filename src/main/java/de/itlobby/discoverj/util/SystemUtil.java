package de.itlobby.discoverj.util;

import de.itlobby.discoverj.models.OperatingSystem;
import de.itlobby.discoverj.models.SearchEngineType;
import de.itlobby.discoverj.searchservice.SearchService;
import de.itlobby.discoverj.settings.AppConfig;
import de.itlobby.discoverj.settings.Settings;
import javafx.scene.Node;
import javafx.scene.layout.FlowPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;

import static de.itlobby.discoverj.util.AudioUtil.VALID_IMAGE_FILE_EXTENSION;

public class SystemUtil {
    private static final Logger log = LogManager.getLogger(SystemUtil.class);

    private SystemUtil() {
    }

    public static SearchService getSearchService(SearchEngineType searchEngineType) {
        SearchService searchService = null;

        try {
            Class<? extends SearchService> clazz = searchEngineType.getServiceClass();
            searchService = clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return searchService;

    }

    public static <T> Object getPrivateField(T targetObject, String fieldName) {
        try {
            Field field = targetObject.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(targetObject);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return null;
    }

    public static Node getChildrenById(FlowPane flowPane, String nodeName) {
        for (Node node : flowPane.getChildren()) {
            if (node.getId().equals(nodeName)) {
                return node;
            }
        }

        return null;
    }

    public static URL getResourceURL(String path) {
        return Thread.currentThread().getContextClassLoader().getResource(path);
    }

    public static void setProxy() {
        AppConfig config = Settings.getInstance().getConfig();

        System.setProperty("http.agent", "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1)");
        System.setProperty("https.agent", "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1)");

        if (config.isProxyEnabled()) {
            System.setProperty("http.proxyHost", config.getProxyUrl());
            System.setProperty("http.proxyPort", config.getProxyPort());
            System.setProperty("http.proxyUser", config.getProxyUser());
            System.setProperty("http.proxyPassword", config.getProxyPassword());

            System.setProperty("https.proxyHost", config.getProxyUrl());
            System.setProperty("https.proxyPort", config.getProxyPort());
            System.setProperty("https.proxyUser", config.getProxyUser());
            System.setProperty("https.proxyPassword", config.getProxyPassword());
        } else {
            System.setProperty("http.proxyHost", "");
            System.setProperty("http.proxyPort", "");
            System.setProperty("http.proxyUser", "");
            System.setProperty("http.proxyPassword", "");

            System.setProperty("https.proxyHost", "");
            System.setProperty("https.proxyPort", "");
            System.setProperty("https.proxyUser", "");
            System.setProperty("https.proxyPassword", "");
        }
    }

    public static void threadSleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }

    public static OperatingSystem getOs() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            return OperatingSystem.WINDOWS;
        } else if (os.contains("mac")) {
            return OperatingSystem.MAC;
        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            return OperatingSystem.UNIX;
        } else if (os.contains("sunos")) {
            return OperatingSystem.SOLARIS;
        } else {
            return OperatingSystem.OTHER;
        }
    }

    public static Date localeDateToDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    public static boolean is64Bit() {
        boolean is64bit;
        if (System.getProperty("os.name").contains("Windows")) {
            is64bit = (System.getenv("ProgramFiles(x86)") != null);
        } else {
            is64bit = (System.getProperty("os.arch").contains("64"));
        }
        return is64bit;
    }

    public static void launchFile(File targetFile) {
        new Thread(() -> {
            try {
                Desktop desktop = Desktop.getDesktop();
                if (targetFile.exists() && desktop.isSupported(Desktop.Action.OPEN)) {
                    if (getOs() == OperatingSystem.UNIX && targetFile.getName().endsWith(".sh")) {
                        Runtime.getRuntime().exec("sh " + targetFile);
                    } else {
                        desktop.open(targetFile);
                    }
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }).start();
    }

    public static void browseUrl(String urlString) {
        try {
            browseUrl(new URL(urlString));
        } catch (MalformedURLException e) {
            log.error(e.getMessage(), e);
        }
    }

    public static void browseUrl(URL url) {
        java.awt.Desktop desktop = java.awt.Desktop.isDesktopSupported() ? java.awt.Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
            new Thread(() -> {
                try {
                    desktop.browse(url.toURI());
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }).start();
        }
    }

    public static File getFileFromFolderChooser(File lastPath) {
        DirectoryChooser chooser = new DirectoryChooser();
        if (lastPath.exists()) {
            chooser.setInitialDirectory(lastPath);
        }
        chooser.setTitle(LanguageUtil.getString("InitialController.changeMusicFolder"));

        return chooser.showDialog(null);
    }

    public static void browseLocalPath(File file) {
        OperatingSystem os = getOs();
        java.awt.Desktop desktop = java.awt.Desktop.isDesktopSupported() ? java.awt.Desktop.getDesktop() : null;

        new Thread(() -> {
            if (desktop != null && desktop.isSupported(Desktop.Action.OPEN) && file.exists()) {
                try {
                    if (os == OperatingSystem.WINDOWS) {
                        new ProcessBuilder("explorer.exe", "/select," + file.getAbsolutePath()).start();
                    } else {
                        desktop.open(file.getParentFile());
                    }
                } catch (Exception e) {
                    log.debug(e.getMessage(), e);
                }
            }
        }).start();
    }

    public static File getImageFileFromFileChooser(String title) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        fileChooser.setTitle(title);
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Jpg", "*.jpg"),
                new FileChooser.ExtensionFilter("Png", "*.png"),
                new FileChooser.ExtensionFilter("Gif", "*.gif"),
                new FileChooser.ExtensionFilter("Bmp", "*.bmp")
        );
        return fileChooser.showOpenDialog(null);
    }

    public static File getTempFileFromUrl(URL url) throws IOException {
        return File.createTempFile("tmp_", "." + StringUtil.getFileExtension(url.getFile()));
    }

    public static boolean isImageFile(File file) {
        return Arrays.asList(VALID_IMAGE_FILE_EXTENSION)
                .contains(StringUtil.getFileExtension(file).toLowerCase());
    }

    public static boolean isImage(String sUrl) {
        try {
            String contentType = new URL(sUrl).openConnection().getContentType();
            return !StringUtil.isNullOrEmpty(contentType) && contentType.split("/")[0].equalsIgnoreCase("image");
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }

        return false;
    }

    public static int getCpuCount() {
        return (int) (Runtime.getRuntime().availableProcessors() / 2.0f);
    }

    public static void requestUserAttentionInTaskbar() {
        if (Taskbar.isTaskbarSupported() && Taskbar.getTaskbar().isSupported(Taskbar.Feature.USER_ATTENTION)) {
            new Thread(() -> Taskbar.getTaskbar().requestUserAttention(true, false)).start();
        }
    }

    public static void setTaskbarProgress(Number value) {
        if (Taskbar.isTaskbarSupported() && Taskbar.getTaskbar().isSupported(Taskbar.Feature.PROGRESS_VALUE)) {
            new Thread(() -> Taskbar.getTaskbar().setProgressValue((int) (value.doubleValue() * 100))).start();
        }
    }
}
