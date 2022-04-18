package de.itlobby.discoverj.viewcontroller;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

public class OpenFileViewController implements ViewController {
    public Button btnExitDialog;
    public Circle circleBorder;
    public Label lblIntroduction;
    public VBox textLayout;
    public AnchorPane rootLayout;

    @Override
    public void initialize() {
        btnExitDialog.setCancelButton(true);
    }
}
