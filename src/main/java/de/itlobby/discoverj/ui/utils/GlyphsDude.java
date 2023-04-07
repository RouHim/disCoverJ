package de.itlobby.discoverj.ui.utils;

/**
 * Copyright (c) 2013-2016 Jens Deters http://www.jensd.de
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

import de.jensd.fx.glyphs.GlyphIcon;
import de.jensd.fx.glyphs.GlyphIcons;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIconView;
import de.jensd.fx.glyphs.materialicons.MaterialIconView;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * @author Jens Deters
 */
public class GlyphsDude {
    private static final Logger log = LogManager.getLogger(GlyphsDude.class);

    static {
        try {
            Font.loadFont(GlyphsDude.class.getResource(FontAwesomeIconView.TTF_PATH).openStream(), 10.0);
            Font.loadFont(GlyphsDude.class.getResource(MaterialDesignIconView.TTF_PATH).openStream(), 10.0);
            Font.loadFont(GlyphsDude.class.getResource(MaterialIconView.TTF_PATH).openStream(), 10.0);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private GlyphsDude() {
        // no instances
    }

    public static Text createIcon(GlyphIcons icon) {
        return GlyphsDude.createIcon(icon, GlyphIcon.DEFAULT_FONT_SIZE);
    }

    public static Text createIcon(GlyphIcons icon, String iconSize) {
        Text text = new Text(icon.unicode());
        text.getStyleClass().add("glyph-icon");
        text.setStyle(String.format("-fx-font-family: %s; -fx-font-size: %s;", icon.fontFamily(), iconSize));
        return text;
    }
}
