package custom;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConvertClusterDataToSequenceData {

    private static final String ARFF_PATH = "custom/taxidata/100taxi-month1/100taxi-month1-weka-20.arff";
    private static final String TAXI_PATH = "custom/taxidata/100taxi-month1/100taxi-month1-training/";

    public static void main(String[] args) throws IOException {
        Map<String, List<String>> splittedLines = read();
        StringBuilder stringBuilder = new StringBuilder();
        for(Map.Entry<String, List<String>> line:splittedLines.entrySet()){
            List<List<String>> partitionedLines = new ArrayList<>();
            for (int i = 0; i < line.getValue().size() - 4; i++) {
                partitionedLines.add(line.getValue().subList(i, i + 5));
            }
            for(List<String> partitioned:partitionedLines){
                stringBuilder.append(String.join(" -1 ", partitioned)).append(" -2");
                stringBuilder.append(System.lineSeparator());
            }
        }
        writeToFile(stringBuilder.toString());
    }

    public static Map<String, List<String>> read() throws IOException {
        List<String> allLines = Files.readAllLines(Paths.get(ArffReader.class.getClassLoader().getResource(ARFF_PATH).getPath()));
        ForkJoinPool forkJoinPool = new ForkJoinPool(12);
        Map<String, ArffRegion> arffRegions = new HashMap<>();
        forkJoinPool.submit(() -> allLines.forEach(line -> {
            String[] regionString = line.split(",");
            if (regionString.length == 4) {
                ArffRegion arffRegion = new ArffRegion(Double.parseDouble(regionString[1]), Double.parseDouble(regionString[2]), regionString[3]);
                arffRegions.put(arffRegion.getxPoint() + "#" + arffRegion.getyPoint(), arffRegion);
            }
        })).join();

        return getRegionListForAllFiles(arffRegions);
    }

    private static Map<String, List<String>> getRegionListForAllFiles(Map<String, ArffRegion> arffRegions) throws IOException {
        String rootFolder = ArffReader.class.getClassLoader().getResource(TAXI_PATH).getPath();
        List<String> files = Stream.of(new File(rootFolder).listFiles())
                .filter(file -> !file.isDirectory())
                .map(File::getName)
                .collect(Collectors.toList());

        Map<String, List<String>> distinctedRegions = new HashMap<>();

        ForkJoinPool forkJoinPool = new ForkJoinPool(12);
        forkJoinPool.submit(() -> files.parallelStream().forEach(file -> {
            InputStream stream = ArffReader.class.getClassLoader().getResourceAsStream(TAXI_PATH + file);
            System.out.println("file " + file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String line = null;
            try {
                line = reader.readLine();
                String lineStringLine = "";
                while (line != null) {
                    lineStringLine = line;
                    line = reader.readLine();
                }
                reader.close();
                LineStringReader lineStringReader = new LineStringReader(lineStringLine);
                lineStringReader.parse();

                List<String> region = new ArrayList<>();
                for (Landmark landmark : lineStringReader.getLandmarks()) {
                    region.add(getRegionByPoints(arffRegions, landmark.getX(), landmark.getY()));
                }

                distinctedRegions.put(file, getDistinctRegions(region));
            } catch (IOException e) {
                e.printStackTrace();
            }

        })).join();
        return distinctedRegions;

    }

    private static List<String> getDistinctRegions(List<String> regions) {
        String lookupRegion = "";
        List<String> updatedTaxiPath = new ArrayList<>();
        for(String region: regions){
            if (!region.equals(lookupRegion) && !region.equals("")) {
                lookupRegion = region;
                updatedTaxiPath.add(region);
            }
        }
        return updatedTaxiPath;
    }

    private static String getRegionByPoints(Map<String, ArffRegion> arffRegions, Double xPoint, Double yPoint) {
        ArffRegion region = arffRegions.get(xPoint + "#" + yPoint);
        if (region != null) {
            return region.getRegion().replace("cluster", "");
        } else {
            return "";
        }
    }

    private static void writeToFile(String content) throws IOException {
        Files.write(Paths.get(ConvertClusterDataToSequenceData.class.getClassLoader().getResource("training-data-100taxi-20.txt").getPath()),
                content.getBytes(), StandardOpenOption.WRITE);
        System.out.println("finished successfully.");
    }
}
