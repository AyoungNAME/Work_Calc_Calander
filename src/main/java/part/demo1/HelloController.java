package part.demo1;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class HelloController {
    // Section: Label connected to the FXML view
    @FXML
    private Label welcomeText;

    // Section: Welcome button action
    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!");
    }
}
