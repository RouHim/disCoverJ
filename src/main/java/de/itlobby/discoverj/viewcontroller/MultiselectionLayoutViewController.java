package de.itlobby.discoverj.viewcontroller;

import de.itlobby.discoverj.framework.ServiceLocator;
import de.itlobby.discoverj.services.InitialService;
import de.itlobby.discoverj.util.helper.AwesomeHelper;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

public class MultiselectionLayoutViewController implements ViewController {
    public Button btnClearCover;
    public Label txtClearCoverText;
    public HBox rootLayout;

    @Override
    public void initialize() {
        AwesomeHelper.createIconButton(btnClearCover, FontAwesomeIcon.TRASH, "sidebar-icon", "", "24px");

        EventHandler<MouseEvent> onClickEvent = event ->
        {
            ServiceLocator.get(InitialService.class).removeAllSelectedCover();
            event.consume();
        };

        rootLayout.addEventHandler(MouseEvent.MOUSE_CLICKED, onClickEvent);
        btnClearCover.addEventHandler(MouseEvent.MOUSE_CLICKED, onClickEvent);
    }

    public void setClearCoverText(String text) {
        Platform.runLater(() ->
                txtClearCoverText.setText(text)
        );
    }
}
