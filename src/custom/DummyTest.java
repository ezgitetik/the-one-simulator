package custom;

import core.Settings;
import org.junit.Assert;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DummyTest {

    public static void main(String[] args) throws IOException {

        List<List<Integer>> sequence = getSequenceByTaxi("taxi-320.wkt");
        int count = 0;
        for (List<Integer> sequenceLine : sequence) {
            if (Collections.indexOfSubList(sequenceLine, Arrays.asList(24, 11)) == 0) {
                count++;
            }
        }
        System.out.println(count > 0 ? "exist" + count : "not exist");
    }

    public static List<List<Integer>> getSequenceByTaxi(String fileName) {
        List<String> allLines = null;
        try {
            allLines = Files.readAllLines(Paths.get(DummyTest.class.getClassLoader().getResource("custom/taxidata/100taxi-month2/100taxi-month2-sequence/" + fileName).getPath()));
            List<List<Integer>> sequenceList = new ArrayList<>();
            for (String line : allLines) {
                String[] sequenceLine = line.split(" ");
                List<Integer> sequenceIntLine = new ArrayList<>();
                for (String value : sequenceLine) {
                    sequenceIntLine.add(Integer.valueOf(value));
                }
                sequenceList.add(sequenceIntLine);
            }
            return sequenceList;
        } catch (IOException e) {
            return null;
        }
    }

}
