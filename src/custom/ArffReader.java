package custom;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ArffReader {

    private static List<ArffRegion> read() throws IOException {

        InputStream stream = ArffReader.class.getClassLoader().getResourceAsStream("data/custom/taxidata/taxi-top10-0101-weka.arff");
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String line = reader.readLine();
        List<ArffRegion> arffRegions = new ArrayList<>();
        while (line != null) {

            String[] regionString = line.split(",");
            if (regionString.length == 4) {
                ArffRegion arffRegion = new ArffRegion(Double.parseDouble(regionString[1]), Double.parseDouble(regionString[2]), regionString[3]);
                arffRegions.add(arffRegion);
                System.out.println(arffRegion.getxPoint());
            }
            line = reader.readLine();
        }
        reader.close();
        return arffRegions;
    }

    public static void main(String[] args) throws IOException {
        List<ArffRegion> arffRegions = ArffReader.read();
    }

}
