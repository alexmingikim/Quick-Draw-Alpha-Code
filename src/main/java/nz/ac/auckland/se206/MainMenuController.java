package nz.ac.auckland.se206;

import com.opencsv.exceptions.CsvException;
import java.io.IOException;
import java.net.URISyntaxException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;

public class MainMenuController {

  @FXML private Button btnSwitchToCanvas;

  @FXML
  private void switchToCanvas(ActionEvent event)
      throws IOException, CsvException, URISyntaxException {
    Button btnClicked = (Button) event.getSource();
    Scene scene = btnClicked.getScene();
    scene.setRoot(SceneManager.getUI(SceneManager.AppUI.CANVAS));
  }
}
