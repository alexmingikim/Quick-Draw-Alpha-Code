package nz.ac.auckland.se206.words;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class CategorySelector {

  public enum Difficulty {
    // difficulty of words either: EASY(E), MEDIUM(M), HARD(H)
    E,
    M,
    H
  }

  // use a map to store which words are of which difficulty
  // type of the key is: Difficulty; the value is a list of strings
  private Map<Difficulty, List<String>> difficulty2Categories;

  public CategorySelector() throws IOException, CsvException, URISyntaxException {
    // constructor to instantiate map
    // initialise a map which will store a list of strings (words) according to difficulty
    difficulty2Categories = new HashMap<>();
    for (Difficulty difficulty : Difficulty.values()) {
      // "for each difficulty", create a list of strings
      difficulty2Categories.put(difficulty, new ArrayList<>()); // at this stage, an empty list
    }
    // input the data
    for (String[] line : getLines()) { // for EACH line read
      // add elements into list we have already created
      difficulty2Categories.get(Difficulty.valueOf(line[1])).add(line[0]);
      // LEFT TO RIGHT: first GET list associated with difficulty
      // then ADD words of that difficulty;
      // difficulty data is stored as the 1st element, and words are
      // stored as 0th element in the file read;
      // convert string to enum
    }
  }

  // given a difficulty input, pick a random string (word) of that category:
  public String getRandomCategory(Difficulty difficulty) {
    return difficulty2Categories
        .get(difficulty)
        .get(new Random().nextInt(difficulty2Categories.get(difficulty).size()));
  }

  protected List<String[]> getLines() throws IOException, CsvException, URISyntaxException {
    // read csv file
    File file = new File(CategorySelector.class.getResource("/category_difficulty.csv").toURI());
    try (FileReader fr = new FileReader(file, StandardCharsets.UTF_8);
        CSVReader reader = new CSVReader(fr)) {
      return reader.readAll();
    }
  }
}
