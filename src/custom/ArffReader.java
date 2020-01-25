package custom;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ArffReader {

    private static List<ArffRegion> ARFF_REGIONS = null;

    public static List<ArffRegion> read() throws IOException {
        if (ARFF_REGIONS == null) {
            InputStream stream = ArffReader.class.getClassLoader().getResourceAsStream("custom/taxidata/taxi-top10-0101-weka.arff");
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String line = reader.readLine();
            List<ArffRegion> arffRegions = new ArrayList<>();
            while (line != null) {

                String[] regionString = line.split(",");
                if (regionString.length == 4) {
                    ArffRegion arffRegion = new ArffRegion(Double.parseDouble(regionString[1]), Double.parseDouble(regionString[2]), regionString[3]);
                    arffRegions.add(arffRegion);
                }
                line = reader.readLine();
            }
            reader.close();
            ARFF_REGIONS = arffRegions;
        }
        return ARFF_REGIONS;
    }

    public static String getRegionByPoints(Double xPoint, Double yPoint) {
        return ARFF_REGIONS.stream()
                .filter(arffRegion -> arffRegion.getxPoint().equals(xPoint) && arffRegion.getyPoint().equals(yPoint))
                .findFirst().orElse(new ArffRegion(0.0, 0.0, "")).getRegion();
    }

    public static List<String> getRegionListByFileName(String fileName) throws IOException {
        InputStream stream = ArffReader.class.getClassLoader().getResourceAsStream("custom/taxidata/bursa-0101/" + fileName);
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String line = reader.readLine();
        String lineStringLine = "";
        while (line != null) {
            lineStringLine = line;
            line = reader.readLine();
        }
        reader.close();
        LineStringReader lineStringReader = new LineStringReader(lineStringLine);
        lineStringReader.parse();
        return lineStringReader.getLandmarks()
                .stream()
                .map(landmark -> ArffReader.getRegionByPoints(landmark.getX(), landmark.getY())).collect(Collectors.toList());
    }

    public static List<String> getDistinctRegionListByFileName(String fileName) throws IOException {
        InputStream stream = ArffReader.class.getClassLoader().getResourceAsStream("custom/taxidata/bursa-0101/" + fileName);
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String line = reader.readLine();
        String lineStringLine = "";
        while (line != null) {
            lineStringLine = line;
            line = reader.readLine();
        }
        reader.close();
        LineStringReader lineStringReader = new LineStringReader(lineStringLine);
        lineStringReader.parse();
        return lineStringReader.getLandmarks()
                .stream()
                .map(landmark -> ArffReader.getRegionByPoints(landmark.getX(), landmark.getY()))
                .filter(distinctByKey(region -> region))
                .sorted()
                .collect(Collectors.toList());
    }

    public static <T> Predicate<T> distinctByKey(
            Function<? super T, ?> keyExtractor) {

        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    public static void main(String[] args) throws IOException {
        List<ArffRegion> arffRegions = ArffReader.read();
        //System.out.println(ArffReader.getRegionByPoints(28183.048, 34760.223));

        List<String> regions = ArffReader.getDistinctRegionListByFileName("taxi-528.wkt");
    }

}
