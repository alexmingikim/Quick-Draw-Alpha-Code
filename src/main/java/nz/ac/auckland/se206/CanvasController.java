package nz.ac.auckland.se206;

import ai.djl.ModelException;
import ai.djl.modality.Classifications;
import ai.djl.modality.Classifications.Classification;
import ai.djl.translate.TranslateException;
import com.opencsv.exceptions.CsvException;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import javax.imageio.ImageIO;
import nz.ac.auckland.se206.ml.DoodlePrediction;
import nz.ac.auckland.se206.speech.TextToSpeech;
import nz.ac.auckland.se206.words.CategorySelector;
import nz.ac.auckland.se206.words.CategorySelector.Difficulty;

/**
 * This is the controller of the canvas. You are free to modify this class and the corresponding
 * FXML file as you see fit. For example, you might no longer need the "Predict" button because the
 * DL model should be automatically queried in the background every second.
 *
 * <p>!! IMPORTANT !!
 *
 * <p>Although we added the scale of the image, you need to be careful when changing the size of the
 * drawable canvas and the brush size. If you make the brush too big or too small with respect to
 * the canvas size, the ML model will not work correctly. So be careful. If you make some changes in
 * the canvas and brush sizes, make sure that the prediction works fine.
 */
public class CanvasController {

  @FXML private Canvas canvas;

  @FXML private Label lblWordToDraw;

  @FXML private Label lblTimeRemaining;

  @FXML private Button btnReady;

  @FXML private Label lblPredictions;

  @FXML private Button btnSpeech;

  private GraphicsContext graphic;

  private DoodlePrediction model;

  private String currentWord; // game should know which word the user is trying to draw

  private static final int TIME_TO_DRAW = 60;

  private static final int NUM_TOP_PREDICTIONS_DISPLAY = 10; // no. of top predictions to display

  private static final int NUM_TOP_PREDICTIONS_WIN = 3; // no. within which correct category must be
  // guessed for user to win

  private Timeline timeline;

  private final IntegerProperty timeSeconds = new SimpleIntegerProperty(TIME_TO_DRAW);

  /**
   * JavaFX calls this method once the GUI elements are loaded. In our case we create a listener for
   * the drawing, and we load the ML model.
   *
   * @throws ModelException If there is an error in reading the input/output of the DL model.
   * @throws IOException If the model cannot be found on the file system.
   * @throws URISyntaxException
   * @throws CsvException
   */
  public void initialize() throws ModelException, IOException, CsvException, URISyntaxException {
    graphic = canvas.getGraphicsContext2D();

    canvas.setOnMouseDragged(
        e -> {
          // Brush size (you can change this, it should not be too small or too large).
          final double size = 5.0;

          final double x = e.getX() - size / 2;
          final double y = e.getY() - size / 2;

          // This is the colour of the brush.
          graphic.setFill(Color.BLACK);
          graphic.fillOval(x, y, size, size);
        });

    model = new DoodlePrediction();

    // *** CHANGE LOCATION OF THIS METHOD: SHOULD BE INVOKED WHEN GAME STARTS ***
    CategorySelector categorySelector = new CategorySelector();
    String randomWord = categorySelector.getRandomCategory(Difficulty.E);
    lblWordToDraw.setText("Please draw: " + randomWord); // label displays random word
    currentWord = randomWord;
  }

  /** This method is called when the "Ready to Draw" button is pressed. */
  @FXML
  private void onReady() {
    lblTimeRemaining.textProperty().bind(timeSeconds.asString());
    canvas.setDisable(false); // enable canvas
    btnReady.setDisable(true);
    timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateTimeAndPredictions()));
    timeline.setCycleCount(TIME_TO_DRAW);
    timeSeconds.set(TIME_TO_DRAW);
    timeline.play();
  }

  private void updateTimeAndPredictions() {
    int seconds = timeSeconds.get();
    timeSeconds.set(seconds - 1);
    BufferedImage image = getCurrentSnapshot(); // application thread must capture the snapshot

    Task<Void> backgroundTask =
        new Task<Void>() {
          @Override
          protected Void call() throws Exception {
            String string = printPredictions(image);
            Platform.runLater(
                () -> {
                  lblPredictions.setText(string);
                });

            // Win condition
            if (isWin(model.getPredictions(image, NUM_TOP_PREDICTIONS_WIN)) == true) {
              timeline.stop();
              Platform.runLater(
                  () -> {
                    lblPredictions.setText("You Won!");
                  });
              finishGame();
            }

            // Lose condition
            if (timeSeconds.get() == 0) {
              Platform.runLater(
                  () -> {
                    lblPredictions.setText("You Lost :(");
                  });
              finishGame();
            }
            return null;
          }
        };
    Thread backgroundThread = new Thread(backgroundTask);
    backgroundThread.start();
  }

  /**
   * Method that prints the top 10 DL model predictions as a string.
   *
   * @param image
   * @return top 10 predictions as a string
   * @throws TranslateException
   */
  private String printPredictions(BufferedImage image) throws TranslateException {
    List<Classifications.Classification> predictions =
        model.getPredictions(image, NUM_TOP_PREDICTIONS_DISPLAY);
    StringBuilder sb = new StringBuilder();
    int i = 1;
    sb.append("Top 10 predictions:\n");
    for (Classifications.Classification classification : predictions) {
      sb.append(i)
          .append(": ")
          .append(classification.getClassName())
          .append(System.lineSeparator());
      i++;
    }
    return sb.toString();
  }

  // User wins if correct category is in top 3 predictions
  private boolean isWin(List<Classification> classifications) {
    for (Classification classification : classifications) {
      if (classification.getClassName().equals(currentWord)) {
        return true;
      }
    }
    return false;
  }

  // This method is called when game is finished;
  private void finishGame() {}

  // Start new game
  private void startGame() {}

  /** This method is called when the "Clear" button is pressed. */
  @FXML
  private void onClear() {
    graphic.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
  }

  // This method is called when the "Text-To-Speech" button is pressed.
  @FXML
  private void onSpeech() {
    TextToSpeech textToSpeech = new TextToSpeech();
    textToSpeech.speak(currentWord); // speak the word which user should draw
  }

  /**
   * This method executes when the user clicks the "Predict" button. It gets the current drawing,
   * queries the DL model and prints on the console the top 5 predictions of the DL model and the
   * elapsed time of the prediction in milliseconds.
   *
   * @throws TranslateException If there is an error in reading the input/output of the DL model.
   */
  @FXML
  private void onPredict() throws TranslateException {
    System.out.println("==== PREDICTION  ====");
    System.out.println("Top 5 predictions");

    final long start = System.currentTimeMillis();

    List<Classification> predictionResults = model.getPredictions(getCurrentSnapshot(), 3);
    // printPredictions(predictionResults);

    System.out.println(isWin(predictionResults) ? "WIN" : "LOSS");

    System.out.println("prediction performed in " + (System.currentTimeMillis() - start) + " ms");
  }

  /**
   * Get the current snapshot of the canvas.
   *
   * @return The BufferedImage corresponding to the current canvas content.
   */
  private BufferedImage getCurrentSnapshot() {
    final Image snapshot = canvas.snapshot(null, null);
    final BufferedImage image = SwingFXUtils.fromFXImage(snapshot, null);

    // Convert into a binary image.
    final BufferedImage imageBinary =
        new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);

    final Graphics2D graphics = imageBinary.createGraphics();

    graphics.drawImage(image, 0, 0, null);

    // To release memory we dispose.
    graphics.dispose();

    return imageBinary;
  }

  /**
   * Save the current snapshot on a bitmap file.
   *
   * @return The file of the saved image.
   * @throws IOException If the image cannot be saved.
   */
  private File saveCurrentSnapshotOnFile() throws IOException {
    // You can change the location as you see fit.
    final File tmpFolder = new File("tmp");

    if (!tmpFolder.exists()) {
      tmpFolder.mkdir();
    }

    // We save the image to a file in the tmp folder.
    final File imageToClassify =
        new File(tmpFolder.getName() + "/snapshot" + System.currentTimeMillis() + ".bmp");

    // Save the image to a file.
    ImageIO.write(getCurrentSnapshot(), "bmp", imageToClassify);

    return imageToClassify;
  }
}
