package nz.ac.auckland.se206.words;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.opencsv.exceptions.CsvException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import org.junit.jupiter.api.Test;

class CategorySelectorTest {

  @Test
  public void testCSVReader() throws IOException, CsvException, URISyntaxException {
    CategorySelector categorySelector = new CategorySelector();
    List<String[]> result = categorySelector.getLines();
    int size = result.size();
    assertTrue(size == 345);
  }
}
