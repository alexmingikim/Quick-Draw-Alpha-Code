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
import java.nio.file.Files;
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
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import javax.imageio.ImageIO;
import nz.ac.auckland.se206.ml.DoodlePrediction;
import nz.ac.auckland.se206.speech.TextToSpeech;
import nz.ac.auckland.se206.words.CategorySelector;

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

  private static final int TIME_TO_DRAW = 60;

  private static final int NUM_TOP_PREDICTIONS_DISPLAY = 10; // no. of top predictions to display

  private static final int NUM_TOP_PREDICTIONS_WIN = 3; // position within which correct
  // word must be guessed for user to win

  @FXML private Canvas canvas;

  @FXML private Label lblWordToDraw;

  @FXML private Label lblTimeRemaining;

  @FXML private Button btnReady;

  @FXML private Label lblPredictions;

  @FXML private Button btnSpeech;

  @FXML private Button btnDraw;

  @FXML private Button btnErase;

  @FXML private Button btnClear;

  @FXML private Button btnSaveDrawing;

  @FXML private Button btnNewGame;

  private GraphicsContext graphic;

  private DoodlePrediction model;

  private String currentWord; // game should know which word the user is trying to draw

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
    model = new DoodlePrediction();
    startGame();
  }

  // This method generates word of specified difficulty.
  private void generateRandomWord(CategorySelector.Difficulty difficulty)
      throws IOException, CsvException, URISyntaxException {
    CategorySelector categorySelector = new CategorySelector();
    String randomWord = categorySelector.getRandomCategory(difficulty);
    lblWordToDraw.setText("Please draw: " + randomWord);
    currentWord = randomWord;
  }

  // Draw
  @FXML
  private void onDraw() {
    btnDraw.setDisable(true);
    btnErase.setDisable(false);
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
  }

  // Erase
  @FXML
  private void onErase() {
    btnDraw.setDisable(false);
    btnErase.setDisable(true);
    canvas.setOnMouseDragged(
        e -> {
          // Brush size (you can change this, it should not be too small or too large).
          final double size = 10.0;

          final double x = e.getX() - size / 2;
          final double y = e.getY() - size / 2;

          // This is the colour of the brush.
          graphic.setFill(Color.WHITE);
          graphic.fillOval(x, y, size, size);
        });
  }

  /** This method is called when the "Ready to Draw" button is pressed. */
  @FXML
  private void onReady() {
    onDraw();
    canvas.setDisable(false);
    btnReady.setDisable(true);
    btnClear.setDisable(false);

    lblTimeRemaining.textProperty().bind(timeSeconds.asString()); // bind time remaining (displayed)
    // to internal timing
    timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateTimeAndPredictions()));
    timeline.setCycleCount(TIME_TO_DRAW);
    timeSeconds.set(TIME_TO_DRAW);
    timeline.play();
  }

  // This method, every second, updates time remaining and top 10 predictions.
  private void updateTimeAndPredictions() {
    int seconds = timeSeconds.get();
    timeSeconds.set(seconds - 1); // count down seconds
    BufferedImage image = getCurrentSnapshot(); // application thread must capture the snapshot

    // Task performed by background thread
    Task<Void> backgroundTask =
        new Task<Void>() {
          @Override
          protected Void call() throws Exception {
            // background thread prints predictions
            String string = printPredictions(image);
            // Application thread prints predictions to GUI
            Platform.runLater(
                () -> {
                  lblPredictions.setText(string);
                });

            // Win condition
            if (isWin(model.getPredictions(image, NUM_TOP_PREDICTIONS_WIN)) == true) {
              timeline.stop();
              // Application thread prints predictions to GUI
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
   * @param image snapshot of canvas
   * @return top 10 predictions as a string
   * @throws TranslateException
   */
  private String printPredictions(BufferedImage image) throws TranslateException {
    // use DL model to get predictions based on current snapshot
    List<Classifications.Classification> predictions =
        model.getPredictions(image, NUM_TOP_PREDICTIONS_DISPLAY);

    // print top 10 predictions as a single string
    StringBuilder sb = new StringBuilder();
    int i = 1;
    sb.append("Top 10 predictions:\n");
    for (Classifications.Classification classification : predictions) {
      sb.append(i)
          .append(": ")
          .append(classification.getClassName().replaceAll("_", " "))
          .append(System.lineSeparator());
      i++;
    }
    return sb.toString();
  }

  // User wins if correct category is in top 3 predictions
  private boolean isWin(List<Classification> classifications) {
    for (Classification classification : classifications) {
      if (classification.getClassName().replaceAll("_", " ").equals(currentWord)) {
        return true;
      }
    }
    return false;
  }

  // This method is called when game is finished;
  private void finishGame() {
    canvas.setDisable(true);

    // enable/disable buttons
    btnDraw.setDisable(true);
    btnErase.setDisable(true);
    btnClear.setDisable(true);
    btnSaveDrawing.setDisable(false);
    btnNewGame.setDisable(false);
  }

  // Start new game
  private void startGame() throws IOException, CsvException, URISyntaxException {
    onClear();
    generateRandomWord(CategorySelector.Difficulty.E); // generate random word of E difficulty

    // enable/disable buttons
    btnDraw.setDisable(true);
    btnErase.setDisable(true);
    btnClear.setDisable(true);
    btnSaveDrawing.setDisable(true);
    btnNewGame.setDisable(true);
    btnReady.setDisable(false);
  }

  /** This method is called when the "Clear" button is pressed. */
  @FXML
  private void onClear() {
    graphic.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
  }

  // This method is called when the "Text-To-Speech" button is pressed.
  @FXML
  private void onProduceSpeech() {

    Task<Void> backgroundTask1 =
        new Task<Void>() {
          @Override
          protected Void call() throws Exception {
            TextToSpeech textToSpeech = new TextToSpeech();
            textToSpeech.speak(currentWord); // speak the word which user should draw
            return null;
          }
        };
    Thread backgroundThread1 = new Thread(backgroundTask1);
    backgroundThread1.start();
  }

  // This method is called when the "Save Drawing" button is pressed.
  @FXML
  private void onSaveDrawing() throws IOException {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Save");
    File file = saveCurrentSnapshotOnFile();
    File file1 = fileChooser.showSaveDialog(new Stage());
    Files.copy(file.toPath(), file1.toPath());
  }

  // This method is called when the "New Game" button is pressed.
  @FXML
  private void onPlayNewGame() throws IOException, CsvException, URISyntaxException {
    startGame();
    btnReady.setDisable(false);
  }

  /**
   * METHOD NOT USED. This method executes when the user clicks the "Predict" button. It gets the
   * current drawing, queries the DL model and prints on the console the top 5 predictions of the DL
   * model and the elapsed time of the prediction in milliseconds.
   *
   * @throws TranslateException If there is an error in reading the input/output of the DL model.
   */
  @FXML
  private void onPredict() throws TranslateException {
    System.out.println("==== PREDICTION  ====");
    System.out.println("Top 5 predictions");

    final long start = System.currentTimeMillis();

    List<Classification> predictionResults = model.getPredictions(getCurrentSnapshot(), 3);

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
