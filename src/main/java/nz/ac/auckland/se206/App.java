package nz.ac.auckland.se206;

import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * This is the entry point of the JavaFX application, while you can change this class, it should
 * remain as the class that runs the JavaFX application.
 */
public class App extends Application {
  public static void main(final String[] args) {
    launch();
  }

  /**
   * Returns the node associated to the input file. The method expects that the file is located in
   * "src/main/resources/fxml".
   *
   * @param fxml The name of the FXML file (without extension).
   * @return The node of the input file.
   * @throws IOException If the file is not found.
   */
  private static Parent loadFxml(final String fxml) throws IOException {
    return new FXMLLoader(App.class.getResource("/fxml/" + fxml + ".fxml")).load();
  }

  /**
   * This method is invoked when the application starts. It loads and shows the "main menu" scene.
   *
   * @param stage The primary stage of the application.
   * @throws IOException If "src/main/resources/fxml/mainmenu.fxml" is not found.
   */
  @Override
  public void start(final Stage stage) throws IOException {
    // load root nodes and store in a map; to be referenced later
    SceneManager.addUi(SceneManager.AppUi.CANVAS, loadFxml("canvas"));
    SceneManager.addUi(SceneManager.AppUi.MAIN_MENU, loadFxml("mainmenu"));

    // changed size of app (default: 640x480); load main menu first
    final Scene scene = new Scene(SceneManager.getUi(SceneManager.AppUi.MAIN_MENU), 640, 480);
    stage.setTitle("Quick, Draw!");
    stage.setScene(scene);
    stage.show();
  }
}
