package nz.ac.auckland.se206;

import java.util.HashMap;
import javafx.scene.Parent;

public class SceneManager {

  public enum AppUi {
    CANVAS,
    MAIN_MENU
  }

  // use a MAP to store and match UI with ONE root node each
  private static HashMap<AppUi, Parent> sceneMap = new HashMap<AppUi, Parent>();

  // associate a UI with its root node
  public static void addUi(AppUi uiType, Parent parentNode) {
    sceneMap.put(uiType, parentNode);
  }

  // get root node for that UI
  public static Parent getUi(AppUi uiType) {
    return sceneMap.get(uiType);
  }
}
