package nz.ac.auckland.se206;

import java.util.HashMap;
import javafx.scene.Parent;

public class SceneManager {

  public enum AppUI {
    CANVAS,
    MAIN_MENU
  }

  // use a MAP to store and match UI with ONE root node each
  private static HashMap<AppUI, Parent> sceneMap = new HashMap<AppUI, Parent>();

  // associate a UI with its root node
  public static void addUI(AppUI UIType, Parent parentNode) {
    sceneMap.put(UIType, parentNode);
  }

  // get root node for that UI
  public static Parent getUI(AppUI UIType) {
    return sceneMap.get(UIType);
  }
}
