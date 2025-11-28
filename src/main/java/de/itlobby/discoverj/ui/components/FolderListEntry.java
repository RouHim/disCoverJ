package de.itlobby.discoverj.ui.components;

import de.itlobby.discoverj.services.InitialService;
import de.itlobby.discoverj.ui.core.ServiceLocator;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.File;
import java.util.Objects;

public class FolderListEntry extends HBox {

    private final String name;
    private final String path;

    public FolderListEntry(String path) {
        this.path = path;
        this.name = new File(path).getName();

        buildLayout();
    }

    private void buildLayout() {
        FontAwesomeIconView iconView = new FontAwesomeIconView(FontAwesomeIcon.FOLDER);
        iconView.setStyle("-fx-font-family: FontAwesome; -fx-font-size: 2em; -fx-text-alignment: center;");
        iconView.setWrappingWidth(36);

        Label nameLabel = new Label(name);
        Label pathLabel = new Label(path);

        VBox labelLayout = new VBox();
        labelLayout.getChildren().add(nameLabel);
        labelLayout.getChildren().add(pathLabel);

        getChildren().add(iconView);
        getChildren().add(labelLayout);

        setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(labelLayout, Priority.ALWAYS);
        HBox.setHgrow(iconView, Priority.NEVER);
        HBox.setMargin(labelLayout, new Insets(5));

        setSpacing(5);
        getStyleClass().add("folder-line");

        setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                ServiceLocator.get(InitialService.class).selectLine(
                        FolderListEntry.this,
                        event.isControlDown(),
                        event.isShiftDown()
                );
            }
        });
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FolderListEntry that = (FolderListEntry) o;
        return Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }

    public String getPath() {
        return path;
    }
}
