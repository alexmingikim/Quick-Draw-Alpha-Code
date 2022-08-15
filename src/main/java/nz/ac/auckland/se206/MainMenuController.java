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
  private void onSwitchToCanvas(ActionEvent event)
      throws IOException, CsvException, URISyntaxException {
    Button btnClicked = (Button) event.getSource();
    Scene scene = btnClicked.getScene();
    // change root node of scene
    scene.setRoot(SceneManager.getUi(SceneManager.AppUi.CANVAS));
  }
}
