package de.itlobby.discoverj.util;

import de.itlobby.discoverj.Main;
import de.itlobby.discoverj.models.Version;
import de.itlobby.discoverj.settings.Settings;
import de.itlobby.discoverj.ui.core.ViewManager;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class VersionDetector {

    private static final Logger log = LogManager.getLogger(VersionDetector.class);
    private static final String DISCOVERJ_VERSION = "discoverj-version";
    private static final String MANIFEST_PATH = "META-INF/MANIFEST.MF";

    private VersionDetector() {}

    public static void determineCurrentVersion() {
        try {
            Enumeration<URL> manifestFiles = Thread.currentThread().getContextClassLoader().getResources(MANIFEST_PATH);

            List<Attributes> attributes = Collections.list(manifestFiles)
                .stream()
                .map(url -> toManifest(url).getMainAttributes())
                .filter(manifestAttributes -> manifestAttributes.containsValue(Main.class.getName()))
                .toList();

            attributes
                .stream()
                .map(manifestAttributes -> manifestAttributes.getValue(DISCOVERJ_VERSION))
                .findFirst()
                .ifPresentOrElse(
                    version -> Settings.getInstance().setVersion(new Version(version)),
                    () -> Settings.getInstance().setVersion(new Version("999999"))
                );

            ViewManager.getInstance().setPrimaryTitle("disCoverJ " + Settings.getInstance().getVersion().toString());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private static Manifest toManifest(URL url) {
        try {
            return new Manifest(url.openStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new Manifest();
    }
}
